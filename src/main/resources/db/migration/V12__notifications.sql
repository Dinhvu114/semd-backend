CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL
        REFERENCES users(id),

    title VARCHAR(255) NOT NULL,

    body TEXT,

    notification_type VARCHAR(50),

    is_read BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_user
ON notifications(user_id);

CREATE INDEX idx_notifications_read
ON notifications(is_read);