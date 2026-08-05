-- Giữ tên trạng thái hiện hữu: PENDING tương đương CREATED,
-- CONFIRMED tương đương VERIFIED.
UPDATE public.dispatch_requests SET status = 'PENDING' WHERE status = 'CREATED';
UPDATE public.dispatch_requests SET status = 'CONFIRMED' WHERE status = 'VERIFIED';

ALTER TABLE public.dispatch_requests ALTER COLUMN status SET DEFAULT 'PENDING';
