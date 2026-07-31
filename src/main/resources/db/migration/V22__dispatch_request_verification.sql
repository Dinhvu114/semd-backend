-- Thêm bước xác minh bắt buộc trước khi phát mission.
ALTER TABLE public.dispatch_requests
    ADD COLUMN verified_at timestamp,
    ADD COLUMN verified_by integer REFERENCES public.users(id) ON DELETE SET NULL,
    ADD COLUMN verification_note text,
    ADD COLUMN rejected_at timestamp,
    ADD COLUMN rejected_by integer REFERENCES public.users(id) ON DELETE SET NULL,
    ADD COLUMN rejection_reason text,
    ADD COLUMN confirmed_address varchar(500),
    ADD COLUMN confirmed_latitude numeric(10,7),
    ADD COLUMN confirmed_longitude numeric(10,7),
    ADD COLUMN confirmed_urgency_level varchar(20);

UPDATE public.dispatch_requests SET status = 'CREATED' WHERE status = 'PENDING';
UPDATE public.dispatch_requests SET status = 'VERIFIED' WHERE status = 'CONFIRMED';

ALTER TABLE public.dispatch_requests ALTER COLUMN status SET DEFAULT 'CREATED';

CREATE INDEX idx_dispatch_requests_verified_by ON public.dispatch_requests(verified_by);
CREATE INDEX idx_dispatch_requests_rejected_by ON public.dispatch_requests(rejected_by);
