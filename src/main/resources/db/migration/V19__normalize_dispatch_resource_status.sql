-- V19: Chuẩn hóa giá trị status trong dispatch_resources
-- Đổi ACTIVE -> AVAILABLE để đồng bộ với Java enum DispatchResourceStatus
UPDATE public.dispatch_resources
SET status = 'AVAILABLE'
WHERE status = 'ACTIVE';
