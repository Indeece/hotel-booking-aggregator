CREATE TABLE transactions (
                        id BIGSERIAL PRIMARY KEY,
                        booking_id BIGINT NOT NULL,
                        user_id BIGINT NOT NULL,
                        amount DECIMAL(10, 2) NOT NULL,
                        status VARCHAR(50) NOT NULL,
                        created_at TIMESTAMP NOT NULL
);