# Hotel Booking Aggregator

A microservice-based hotel booking platform built from scratch to explore how independent Spring Boot services can talk to each other reliably — through synchronous REST calls where consistency matters immediately, and through Kafka events where it doesn't.

The system covers the full lifecycle of a booking: a user registers, browses hotels, reserves a room, gets charged, and receives an email confirming whether the payment went through — with each of those steps owned by a different service and a different database.

## Why this project exists

Most tutorial-style microservice demos stop at "service A calls service B." This one goes a step further and works through the problems you actually run into once you wire real services together over Docker networks and a real Kafka broker: consumers crashing on class name mismatches across service boundaries, `depends_on` not working the way you'd expect across separate compose files, Hibernate schema validation catching drift between an entity and the database, and a payment amount that started life as a hardcoded placeholder before being wired up to a real pricing lookup. None of that shows up in a "hello world" microservices tutorial, and figuring it out was most of the actual work here.

## Architecture

```
                         ┌─────────────┐
                         │ API Gateway │  (Spring Cloud Gateway)
                         │  :4000      │  validates JWT, injects
                         └──────┬──────┘  X-User-Id / X-User-Role
                                │
        ┌───────────────┬──────┴────────┬────────────────┐
        ▼               ▼               ▼                
┌───────────────┐ ┌─────────────┐ ┌────────────────┐
│  auth-service  │ │hotel-service│ │ booking-service│
│     :8081      │ │    :8082    │ │     :8083      │
│ Postgres+Redis │ │Postgres+Redis│ │   Postgres     │
└───────────────┘ └─────────────┘ └───────┬────────┘
                                            │ order-created-events
                                            ▼
                                   ┌─────────────────┐
                                   │ payment-service │
                                   │     :8084       │
                                   │    Postgres     │
                                   └────────┬────────┘
                                            │ payment-success/failed-events
                              ┌─────────────┴─────────────┐
                              ▼                           ▼
                    ┌───────────────────┐         ┌──────────────┐
                    │ booking-service    │         │notification- │
                    │ (updates status)   │         │  service     │
                    └───────────────────┘         │   :8085      │
                                                    │ + Mailpit    │
                                                    └──────────────┘
```

Everything user-facing goes through the **API Gateway** (Spring Cloud Gateway), which is the only service that understands JWTs. Routes are split into public and secured groups: `/api/v1/auth/register`, `/login`, and `/refresh` pass straight through, while everything else — user profile/balance, hotels, bookings — goes through a custom `JwtAuthenticationFilter` first. The filter validates the token and rewrites the request with two headers, `X-User-Id` and `X-User-Role`; every internal service trusts those headers rather than re-validating tokens itself. That keeps auth logic in exactly one place.

One thing worth knowing if you're testing hotel search directly: `hotel-service`'s own controller allows anonymous `GET` requests for browsing hotels and rooms, but the gateway route for `/api/v1/hotels/**` currently applies the JWT filter to everything, GETs included. So browsing hotels through the gateway still requires a token today, even though the service itself doesn't need one — worth splitting into a public/secured route pair the same way auth-service is set up, if anonymous browsing through the gateway matters.

Everything past the booking step is event-driven. `booking-service` doesn't know or care whether a payment succeeds — it fires an event and moves on. `payment-service` doesn't know or care who needs to be notified — it just reports the outcome. That decoupling is the actual point of using Kafka here rather than chaining synchronous calls.

## Services

| Service | Port | Database | Responsibility |
|---|---|---|---|
| `api-gateway` | 4000 | — | Single entry point, JWT validation, request routing |
| `auth-service` | 8081 | Postgres + Redis | Registration, login, JWT issuing, refresh tokens, user balance |
| `hotel-service` | 8082 | Postgres + Redis | Hotel and room catalog (CRUD, cached lookups) |
| `booking-service` | 8083 | Postgres | Booking creation, availability checks, booking status |
| `payment-service` | 8084 | Postgres | Charges the user's balance for a booking, records transactions |
| `notification-service` | 8085 | — | Sends email confirmations via Mailpit |

Auth and Hotel each keep a Redis cache alongside Postgres — Auth uses it for refresh tokens, Hotel uses it to cache hotel/room lookups (`@Cacheable` on city search and hotel details, invalidated on any write).

## The booking flow, end to end

This is the part that took the most iteration to get right, so it's worth spelling out:

1. **Client** calls `POST /api/v1/bookings` through the gateway with a JWT. The gateway resolves the token into `X-User-Id` / `X-User-Role` headers.
2. **booking-service** checks the requested dates don't overlap an existing booking for that room, saves the booking as `PENDING`, and publishes an `OrderCreatedEvent` (`bookingId`, `userId`, `roomId`) to the `order-created-events` topic.
3. **payment-service** picks up the event, calls `hotel-service` (via Feign) to look up the room's actual `pricePerNight`, then calls `auth-service` to deduct that amount from the user's balance.
4. Depending on the outcome, `payment-service` publishes a `PaymentResultEvent` to either `payment-success-events` or `payment-failed-events`.
5. **booking-service** and **notification-service** both consume those topics independently — booking-service flips the booking to `CONFIRMED` or `REJECTED`, notification-service sends an email through Mailpit.

Nobody in this chain calls anybody else back synchronously after step 2. If `notification-service` is down, bookings still get confirmed and charged correctly; the email just goes out once it's back up.

### A note on cross-service Kafka payloads

Each service defines its own local copy of `OrderCreatedEvent` / `PaymentResultEvent` in its own package rather than sharing a DTO module. That's a deliberate simplification for a project this size, but it has one real consequence: Spring Kafka's default `JsonDeserializer` behavior is to read the sender's fully-qualified class name from a message header and look for that exact class on the receiving side — which fails immediately when the receiving service lives in a different package. Every consumer here is configured with `ErrorHandlingDeserializer` and an explicit `spring.json.value.default.type`, so deserialization always targets the local DTO regardless of what the producer's class was called. If you add a new event type, remember to register it the same way rather than relying on the type header.

## API Reference

All endpoints below are called through the gateway (`http://localhost:4000`), which handles JWT verification. Where a header column is empty, no auth is required.

### auth-service — `/api/v1/auth`

| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/register` | `{ username, password }` | New users start with a balance of 10,000 and role `CLIENT` |
| POST | `/login` | `{ username, password }` | Returns `{ accessToken, refreshToken }` |
| POST | `/refresh` | query params `userId`, `refreshToken` | Refresh tokens are stored in Redis, single active token per user |

### auth-service — `/api/v1/users`

| Method | Path | Headers | Notes |
|---|---|---|---|
| GET | `/me` | `X-User-Id` | Returns the caller's profile (password stripped) |
| PUT | `/{id}/balance/deduct` | — | query param `amount`; used internally by payment-service |
| PUT | `/{id}/balance/deposit` | `X-User-Id`, `X-User-Role` | Only the account owner or a `MANAGER` can top up a balance |

### hotel-service — `/api/v1/hotels`

| Method | Path | Role required | Notes |
|---|---|---|---|
| GET | `/?city=...` | — | Cached per city |
| GET | `/{id}` | — | Cached per hotel |
| POST | `/create` | `MANAGER` | |
| PUT | `/update/{id}` | `MANAGER` | |
| DELETE | `/{id}` | `MANAGER` | |

### hotel-service — rooms

| Method | Path | Role required | Notes |
|---|---|---|---|
| GET | `/api/v1/hotels/{hotelId}/rooms` | — | |
| GET | `/api/v1/rooms/{id}` | — | Also called internally by payment-service to price a booking |
| POST | `/api/v1/hotels/{hotelId}/rooms/create` | `MANAGER` | |
| PUT | `/api/v1/rooms/update/{id}` | `MANAGER` | |
| DELETE | `/api/v1/rooms/{id}` | `MANAGER` | |

### booking-service — `/api/v1/bookings`

| Method | Path | Headers | Notes |
|---|---|---|---|
| POST | `/` | `X-User-Id` | Validates dates, checks overlap, publishes `OrderCreatedEvent` |

Booking status moves through `PENDING → CONFIRMED / REJECTED` automatically once payment resolves. `CANCELLED` exists as a status but isn't wired to an endpoint yet.

## Tech stack

- **Java 21**, Spring Boot 3.3.4, built with **Gradle**
- **Spring Cloud 2023.0.3**, **Spring Cloud Gateway** (reactive) for routing and JWT enforcement
- **Spring Data JPA** + PostgreSQL, one schema per service, with **Liquibase** managing migrations
- **Redis** for auth refresh tokens and hotel-service caching
- **Apache Kafka** (KRaft mode, no Zookeeper) for async events between booking, payment, and notification
- **OpenFeign** for the synchronous calls payment-service makes to auth-service and hotel-service
- **JJWT** for token signing/parsing, HMAC-SHA256
- **Spring Boot Actuator + Micrometer/Prometheus** exposed on each service for health checks and metrics scraping
- **Mailpit** as a local SMTP catcher — no real emails leave the network, so you can test the notification flow without configuring a mail provider
- **Docker Compose** to run the whole stack locally

## Running it locally

Each service also ships its own standalone `docker-compose.yml` for working on it in isolation, but the straightforward way to run the whole system is the root compose file, which brings up every database, Kafka, and all six services in the right order:

```bash
git clone https://github.com/Indeece/hotel-booking-aggregator.git
cd hotel-booking-aggregator
docker compose up --build
```

Give it a minute — Postgres and Kafka need to pass their health checks before the dependent services are allowed to start, so the first boot is the slowest one.

Once everything is up:

| What | Where |
|---|---|
| API Gateway | http://localhost:4000 |
| Mailpit inbox (see the confirmation emails) | http://localhost:8025 |
| Auth / Hotel / Booking / Payment / Notification, if you need to hit a service directly | :8081 / :8082 / :8083 / :8084 / :8085 |

A typical smoke test:

```bash
# 1. Register and log in
curl -X POST localhost:4000/api/v1/auth/register -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test123"}'

curl -X POST localhost:4000/api/v1/auth/login -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test123"}'
# → save the accessToken from the response

# 2. Create a hotel + room as a MANAGER (skip if seed data exists)

# 3. Book a room
curl -X POST localhost:4000/api/v1/bookings \
  -H "Authorization: Bearer <accessToken>" -H "Content-Type: application/json" \
  -d '{"hotelId":1,"roomId":1,"startDate":"2026-08-01","endDate":"2026-08-05"}'

# 4. Watch it happen live
docker compose logs -f booking-service payment-service notification-service
```

Then check http://localhost:8025 — the confirmation email should show up within a couple of seconds.

## Project structure

```
hotel-booking-aggregator/
├── api-gateway/
├── auth-service/
├── hotel-service/
├── booking-service/
├── payment-service/
├── notification-service/
└── docker-compose.yml   # root compose, runs everything together
```

## Known limitations / what's next

This was built in a few days as a focused exercise in getting the inter-service plumbing right, so a few things are intentionally left rough:

- **No shared event schema.** As noted above, event DTOs are duplicated per service. Worth extracting into a shared library (or a schema registry) if this grows past a demo.
- **No automated tests yet.** Unit and integration tests are the next thing being added.
- **Booking cancellation** has a status but no endpoint to trigger it.
- **JWT secret and DB credentials are plaintext in compose files** — fine for local dev, not something to carry into any real deployment without a secrets manager.
- **No API gateway rate limiting / circuit breaking** — Feign calls between payment-service and its dependencies currently just log and mark the transaction as failed on any exception, without retry or fallback logic.