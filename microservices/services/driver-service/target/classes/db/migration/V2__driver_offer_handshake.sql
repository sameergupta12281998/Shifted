CREATE TABLE driver_offers (
    id UUID PRIMARY KEY,
    external_offer_id VARCHAR(64) NOT NULL UNIQUE,
    driver_id UUID NOT NULL,
    booking_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_driver_offers_driver ON driver_offers(driver_id);
CREATE INDEX idx_driver_offers_booking ON driver_offers(booking_id);
CREATE INDEX idx_driver_offers_status_expiry ON driver_offers(status, expires_at);
