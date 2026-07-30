-- Thêm các cột timeline còn thiếu vào dispatch_missions
ALTER TABLE dispatch_missions
    ADD COLUMN IF NOT EXISTS reject_reason      TEXT,
    ADD COLUMN IF NOT EXISTS en_route_at        TIMESTAMP,
    ADD COLUMN IF NOT EXISTS arrived_scene_at   TIMESTAMP,
    ADD COLUMN IF NOT EXISTS start_transport_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS arrived_hospital_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS cancelled_at       TIMESTAMP,
    ADD COLUMN IF NOT EXISTS cancel_reason      TEXT;

-- Cập nhật constraint status mới
ALTER TABLE dispatch_missions
DROP CONSTRAINT IF EXISTS dispatch_missions_status_check;

ALTER TABLE dispatch_missions
    ADD CONSTRAINT dispatch_missions_status_check
        CHECK (status IN (
                          'DISPATCHED','ACCEPTED','REJECTED',
                          'EN_ROUTE','ARRIVED_SCENE','TRANSPORTING',
                          'ARRIVED_HOSPITAL','COMPLETED','CANCELLED'
            ));