-- Cập nhật check constraint role cho đúng 5 role đã chốt
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check
    CHECK (role IN ('ADMIN', 'PROVIDER', 'DISPATCHER', 'DRIVER', 'REPORTER'));

-- Bảng dispatch_missions (nếu chưa có từ V1)
CREATE TABLE IF NOT EXISTS dispatch_missions (
                                                 id          SERIAL PRIMARY KEY,
                                                 request_id  INT REFERENCES dispatch_requests(id) UNIQUE NOT NULL,
    resource_id INT REFERENCES dispatch_resources(id) NOT NULL,
    destination_id   INT REFERENCES medical_hospitals(id) ON DELETE SET NULL,
    destination_name VARCHAR(255),
    status      VARCHAR(20) DEFAULT 'DISPATCHED',
    -- Timeline KPI
    dispatched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    accepted_at   TIMESTAMP,
    on_scene_at   TIMESTAMP,
    completed_at  TIMESTAMP,
    notes         TEXT
    );