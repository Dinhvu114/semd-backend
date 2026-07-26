-- định danh người gọi bằng id --
ALTER TABLE emergency_calls
ADD COLUMN reporter_user_id BIGINT
REFERENCES users(id);

CREATE INDEX idx_emergency_calls_reporter
ON emergency_calls(reporter_user_id);

-- provider --
ALTER TABLE providers
ADD COLUMN average_rating NUMERIC(3,2) DEFAULT 0.00; -- đánh giá số sao từ khách hàng --

ALTER TABLE providers
ADD COLUMN review_count INTEGER DEFAULT 0; -- số lượt đánh giá --

ALTER TABLE providers
ADD COLUMN verified_at TIMESTAMPTZ; 

ALTER TABLE providers
ADD COLUMN status VARCHAR(30) DEFAULT 'ACTIVE';

CREATE INDEX idx_provider_status
ON providers(status);

-- review --
ALTER TABLE reviews
ADD COLUMN provider_id UUID
REFERENCES providers(id);

CREATE INDEX idx_reviews_provider
ON reviews(provider_id);

ALTER TABLE reviews
ADD COLUMN provider_reply TEXT;

ALTER TABLE reviews
ADD COLUMN replied_at TIMESTAMPTZ;