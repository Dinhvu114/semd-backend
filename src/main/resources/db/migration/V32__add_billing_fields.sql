-- Bổ sung trường billing snapshot vào payment_transactions
ALTER TABLE payment_transactions
    ADD COLUMN IF NOT EXISTS driver_id INTEGER REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS service_type_code VARCHAR(30),
    ADD COLUMN IF NOT EXISTS billable_distance_km NUMERIC(10,2),
    ADD COLUMN IF NOT EXISTS base_fare NUMERIC(12,2),
    ADD COLUMN IF NOT EXISTS price_per_km NUMERIC(12,2),
    ADD COLUMN IF NOT EXISTS distance_fare NUMERIC(12,2),
    ADD COLUMN IF NOT EXISTS driver_amount NUMERIC(12,2),
    ADD COLUMN IF NOT EXISTS provider_amount NUMERIC(12,2);

-- Đảm bảo mỗi mission chỉ có 1 payment (idempotency)
CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_transaction_mission
    ON payment_transactions(mission_id)
    WHERE mission_id IS NOT NULL;