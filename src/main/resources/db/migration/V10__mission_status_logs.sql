CREATE TABLE mission_status_logs (
    id BIGSERIAL PRIMARY KEY,

    mission_id BIGINT NOT NULL
        REFERENCES dispatch_missions(id),

    old_status VARCHAR(50),

    new_status VARCHAR(50) NOT NULL,

    changed_by BIGINT
        REFERENCES users(id),

    note TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mission_status_logs_mission
ON mission_status_logs(mission_id);