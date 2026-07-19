CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,

    mission_id BIGINT NOT NULL
        REFERENCES dispatch_missions(id),

    reviewer_id BIGINT NOT NULL
        REFERENCES users(id),

    rating INTEGER NOT NULL
        CHECK (rating BETWEEN 1 AND 5),

    comment TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(mission_id, reviewer_id)
);
