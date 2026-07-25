-- Bảng phiên mô phỏng xe cấp cứu
CREATE TABLE ambulance_simulations (
                                       id                    BIGSERIAL PRIMARY KEY,
                                       mission_id            INT NOT NULL REFERENCES dispatch_missions(id),
                                       resource_id           INT NOT NULL REFERENCES dispatch_resources(id),
                                       hospital_id           INT NOT NULL REFERENCES medical_hospitals(id),
                                       source_type           VARCHAR(20)  NOT NULL DEFAULT 'SIMULATION',
                                       status                VARCHAR(20)  NOT NULL DEFAULT 'READY',
                                       phase                 VARCHAR(30)  NOT NULL DEFAULT 'TO_SCENE',
                                       tick_interval_ms      INT          NOT NULL DEFAULT 1000,
                                       speed_multiplier      NUMERIC(5,2) NOT NULL DEFAULT 1.0,
                                       scene_wait_seconds    INT                   DEFAULT 5,
                                       current_location      GEOGRAPHY(Point, 4326),
                                       route_index           INT          NOT NULL DEFAULT 0,
                                       elapsed_route_ms      BIGINT       NOT NULL DEFAULT 0,
                                       distance_travelled_m  NUMERIC(12,2)         DEFAULT 0,
                                       last_tick_at          TIMESTAMPTZ,
                                       started_at            TIMESTAMPTZ,
                                       stopped_at            TIMESTAMPTZ,
                                       completed_at          TIMESTAMPTZ,
                                       error_code            VARCHAR(50),
                                       error_message         VARCHAR(500),
                                       version               BIGINT       NOT NULL DEFAULT 0,
                                       created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                                       updated_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

                                       CONSTRAINT chk_tick_interval   CHECK (tick_interval_ms >= 250),
                                       CONSTRAINT chk_speed_mult      CHECK (speed_multiplier > 0)
);

-- Chỉ 1 phiên READY/RUNNING/STOPPED cho mỗi xe
CREATE UNIQUE INDEX idx_sim_resource_active
    ON ambulance_simulations (resource_id)
    WHERE status IN ('READY', 'RUNNING', 'STOPPED');

CREATE INDEX idx_sim_mission    ON ambulance_simulations (mission_id);
CREATE INDEX idx_sim_res_status ON ambulance_simulations (resource_id, status);
CREATE INDEX idx_sim_status_upd ON ambulance_simulations (status, updated_at);

-- Bảng chặng đường
CREATE TABLE simulation_legs (
                                 id              BIGSERIAL PRIMARY KEY,
                                 simulation_id   BIGINT       NOT NULL REFERENCES ambulance_simulations(id),
                                 leg_type        VARCHAR(30)  NOT NULL,  -- TO_SCENE / TO_HOSPITAL
                                 sequence_no     SMALLINT     NOT NULL,  -- 1 hoặc 2
                                 origin          GEOGRAPHY(Point, 4326),
                                 destination     GEOGRAPHY(Point, 4326),
                                 route_geometry  GEOGRAPHY(LineString, 4326),
                                 route_payload   JSONB,
                                 distance_m      NUMERIC(12,2),
                                 duration_s      NUMERIC(10,2),
                                 created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_leg_simulation ON simulation_legs (simulation_id);

-- Mở rộng resource_location_logs
ALTER TABLE resource_location_logs
    ADD COLUMN IF NOT EXISTS simulation_id BIGINT REFERENCES ambulance_simulations(id),
    ADD COLUMN IF NOT EXISTS mission_id    INT    REFERENCES dispatch_missions(id),
    ADD COLUMN IF NOT EXISTS source_type   VARCHAR(20) DEFAULT 'REAL_GPS',
    ADD COLUMN IF NOT EXISTS heading       NUMERIC(6,2),
    ADD COLUMN IF NOT EXISTS accuracy_m    NUMERIC(8,2);