ALTER TABLE drivers ADD COLUMN account_id UUID;
ALTER TABLE drivers ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX uq_drivers_account_id
    ON drivers(account_id)
    WHERE account_id IS NOT NULL;

ALTER TABLE driver_offers ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE driver_offers ADD CONSTRAINT uq_driver_offers_driver_booking UNIQUE (driver_id, booking_id);