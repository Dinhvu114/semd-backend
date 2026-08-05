-- 1. Đổi edge_nodes -> operation_zones và node_name -> zone_name
ALTER TABLE public.edge_nodes RENAME TO operation_zones;
ALTER TABLE public.operation_zones RENAME COLUMN node_name TO zone_name;

-- 2. Đổi edge_node_id -> zone_id trong dispatch_resources
ALTER TABLE public.dispatch_resources RENAME COLUMN edge_node_id TO zone_id;

-- 3. Xóa is_synced_to_cloud trong dispatch_requests
ALTER TABLE public.dispatch_requests DROP COLUMN IF EXISTS is_synced_to_cloud;

-- 4. Đổi call_end_time -> call_duration (kiểu integer) trong emergency_calls
ALTER TABLE public.emergency_calls RENAME COLUMN call_end_time TO call_duration;
ALTER TABLE public.emergency_calls ALTER COLUMN call_duration TYPE integer USING NULL::integer;

-- 5. Drop các bảng: user_organizations, reviews, emergency_patient_records
DROP TABLE IF EXISTS public.user_organizations CASCADE;
DROP TABLE IF EXISTS public.reviews CASCADE;
DROP TABLE IF EXISTS public.emergency_patient_records CASCADE;

-- 6. Cập nhật các cột bổ sung cho integration_logs
ALTER TABLE public.integration_logs ADD COLUMN IF NOT EXISTS request_id VARCHAR(255);
ALTER TABLE public.integration_logs ADD COLUMN IF NOT EXISTS latency_ms INTEGER;
ALTER TABLE public.integration_logs ADD COLUMN IF NOT EXISTS endpoint VARCHAR(255);

-- 7. Bổ sung các cột nghiệp vụ mới cho dispatch_requests
ALTER TABLE public.dispatch_requests ADD COLUMN IF NOT EXISTS confirmed_by INTEGER REFERENCES public.users(id);
ALTER TABLE public.dispatch_requests ADD COLUMN IF NOT EXISTS confirmed_at TIMESTAMP;
ALTER TABLE public.dispatch_requests ADD COLUMN IF NOT EXISTS ai_confidence NUMERIC(5,2);
ALTER TABLE public.dispatch_requests ADD COLUMN IF NOT EXISTS triage_level VARCHAR(50);
ALTER TABLE public.dispatch_requests ADD COLUMN IF NOT EXISTS review_note TEXT;

-- 8. Thay đổi default status của dispatch_missions sang 'CREATED'
ALTER TABLE public.dispatch_missions ALTER COLUMN status SET DEFAULT 'CREATED';

-- 9. Loại bỏ ràng buộc UNIQUE(request_id) trên dispatch_missions để cho phép quan hệ Many-To-One
ALTER TABLE public.dispatch_missions DROP CONSTRAINT IF EXISTS dispatch_missions_request_id_key;
