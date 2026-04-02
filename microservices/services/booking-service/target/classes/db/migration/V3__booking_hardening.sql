ALTER TABLE bookings ADD COLUMN idempotency_key VARCHAR(80);
ALTER TABLE bookings ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX uq_bookings_user_idempotency
    ON bookings(user_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX idx_bookings_assignment_due ON bookings(status, next_assignment_at);

ALTER TABLE booking_driver_offers ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE booking_driver_offers ADD CONSTRAINT uq_booking_driver_offers_external_offer UNIQUE (external_offer_id);
ALTER TABLE booking_driver_offers ADD CONSTRAINT uq_booking_driver_offers_booking_driver UNIQUE (booking_id, driver_id);