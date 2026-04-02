ALTER TABLE bookings
    ADD COLUMN assignment_attempt INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN next_assignment_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE booking_driver_offers (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL,
    driver_id UUID NOT NULL,
    external_offer_id VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    offered_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_booking_driver_offers_booking_id ON booking_driver_offers(booking_id);
CREATE INDEX idx_booking_driver_offers_driver_id ON booking_driver_offers(driver_id);
CREATE UNIQUE INDEX uq_booking_driver_offers_external_offer_id ON booking_driver_offers(external_offer_id);
