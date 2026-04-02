CREATE TABLE drivers (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    vehicle_type VARCHAR(30) NOT NULL,
    vehicle_number VARCHAR(25) NOT NULL UNIQUE,
    online BOOLEAN NOT NULL,
    available BOOLEAN NOT NULL,
    current_booking_id UUID,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_drivers_lookup ON drivers(vehicle_type, online, available);
