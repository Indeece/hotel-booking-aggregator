CREATE TABLE hotels (
                    id BIGSERIAL PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    description TEXT,
                    city VARCHAR(255) NOT NULL,
                    address VARCHAR(255) NOT NULL,
                    stars INT CHECK (stars < 6 AND stars > 0),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE rooms (
                    id BIGSERIAL PRIMARY KEY,
                    hotel_id BIGINT NOT NULL REFERENCES hotels(id) ON DELETE CASCADE,
                    room_number VARCHAR(50) NOT NULL,
                    type VARCHAR(50) NOT NULL,
                    price_per_night DECIMAL(10, 2) NOT NULL,
                    capacity INT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE (hotel_id, room_number)
);

CREATE INDEX idx_hotels_city ON hotels(city);