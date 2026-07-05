CREATE TABLE bookings (
                          id BIGSERIAL PRIMARY KEY,
                          user_id BIGINT NOT NULL,
                          hotel_id BIGINT NOT NULL,
                          room_id BIGINT NOT NULL,
                          start_date DATE NOT NULL,
                          end_date DATE NOT NULL,
                          status VARCHAR(50) NOT NULL,
                          created_at TIMESTAMP NOT NULL
);