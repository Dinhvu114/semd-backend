-- Chuẩn hóa trạng thái theo các enum hiện tại của backend.
-- EmergencyCall.call_type lưu loại yêu cầu (SOS/CALL), tách biệt với vòng đời xử lý.

ALTER TABLE public.emergency_calls
    ADD COLUMN call_type VARCHAR(20);

-- Bảo tồn nguyên vẹn các giá trị loại cuộc gọi từng được lưu nhầm trong status.
UPDATE public.emergency_calls
SET call_type = status
WHERE status IN ('SOS', 'CALL');

-- Dữ liệu lịch sử không có loại: bản ghi có âm thanh là CALL, còn lại là SOS.
UPDATE public.emergency_calls
SET call_type = CASE
                    WHEN audio_url IS NOT NULL THEN 'CALL'
                    ELSE 'SOS'
                END
WHERE call_type IS NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM public.emergency_calls
        WHERE call_type NOT IN ('SOS', 'CALL')
           OR call_type IS NULL
    ) THEN
        RAISE EXCEPTION 'Không thể xác định call_type cho toàn bộ emergency_calls';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM public.emergency_calls
        WHERE status IS NULL
           OR status NOT IN (
                'SOS', 'CALL', 'ANALYZED',
                'RECEIVED', 'AI_PROCESSING', 'AI_ANALYZED',
                'PENDING_REVIEW', 'CONFIRMED', 'REJECTED', 'CLOSED'
           )
    ) THEN
        RAISE EXCEPTION 'emergency_calls có status không được backend hỗ trợ';
    END IF;
END
$$;

UPDATE public.emergency_calls
SET status = CASE status
                 WHEN 'SOS' THEN 'CONFIRMED'
                 WHEN 'CALL' THEN 'RECEIVED'
                 WHEN 'ANALYZED' THEN 'AI_ANALYZED'
                 ELSE status
             END;

ALTER TABLE public.emergency_calls
    ALTER COLUMN call_type SET NOT NULL,
    ALTER COLUMN status SET DEFAULT 'RECEIVED',
    ALTER COLUMN status SET NOT NULL;

ALTER TABLE public.emergency_calls
    DROP CONSTRAINT IF EXISTS emergency_calls_call_type_check,
    DROP CONSTRAINT IF EXISTS emergency_calls_status_check;

ALTER TABLE public.emergency_calls
    ADD CONSTRAINT emergency_calls_call_type_check
        CHECK (call_type IN ('SOS', 'CALL')),
    ADD CONSTRAINT emergency_calls_status_check
        CHECK (status IN (
            'RECEIVED', 'AI_PROCESSING', 'AI_ANALYZED',
            'PENDING_REVIEW', 'CONFIRMED', 'REJECTED', 'CLOSED'
        ));

-- Dispatch request
UPDATE public.dispatch_requests
SET status = CASE status
                 WHEN 'CREATED' THEN 'PENDING'
                 WHEN 'VERIFIED' THEN 'CONFIRMED'
                 WHEN 'APPROVED' THEN 'CONFIRMED'
                 ELSE status
             END
WHERE status IN ('CREATED', 'VERIFIED', 'APPROVED');

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM public.dispatch_requests
        WHERE status IS NULL
           OR status NOT IN (
                'PENDING', 'CONFIRMED', 'RECOMMENDING', 'DISPATCHING',
                'DISPATCHED', 'COMPLETED', 'REJECTED', 'CANCELLED', 'FAILED'
           )
    ) THEN
        RAISE EXCEPTION 'dispatch_requests có status không được backend hỗ trợ';
    END IF;
END
$$;

ALTER TABLE public.dispatch_requests
    ALTER COLUMN status SET DEFAULT 'PENDING',
    ALTER COLUMN status SET NOT NULL,
    DROP CONSTRAINT IF EXISTS dispatch_requests_status_check;

ALTER TABLE public.dispatch_requests
    ADD CONSTRAINT dispatch_requests_status_check
        CHECK (status IN (
            'PENDING', 'CONFIRMED', 'RECOMMENDING', 'DISPATCHING',
            'DISPATCHED', 'COMPLETED', 'REJECTED', 'CANCELLED', 'FAILED'
        ));

-- Dispatch mission: backend bắt đầu vòng đời mission từ DISPATCHED.
UPDATE public.dispatch_missions
SET status = CASE status
                 WHEN 'CREATED' THEN 'DISPATCHED'
                 WHEN 'ASSIGNED' THEN 'DISPATCHED'
                 WHEN 'DECLINED' THEN 'REJECTED'
                 WHEN 'ARRIVED' THEN 'ARRIVED_SCENE'
                 ELSE status
             END
WHERE status IN ('CREATED', 'ASSIGNED', 'DECLINED', 'ARRIVED');

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM public.dispatch_missions
        WHERE status IS NULL
           OR status NOT IN (
                'DISPATCHED', 'ACCEPTED', 'REJECTED', 'EN_ROUTE',
                'ARRIVED_SCENE', 'TRANSPORTING', 'ARRIVED_HOSPITAL',
                'COMPLETED', 'CANCELLED'
           )
    ) THEN
        RAISE EXCEPTION 'dispatch_missions có status không được backend hỗ trợ';
    END IF;
END
$$;

ALTER TABLE public.dispatch_missions
    ALTER COLUMN status SET DEFAULT 'DISPATCHED',
    ALTER COLUMN status SET NOT NULL,
    DROP CONSTRAINT IF EXISTS dispatch_missions_status_check;

ALTER TABLE public.dispatch_missions
    ADD CONSTRAINT dispatch_missions_status_check
        CHECK (status IN (
            'DISPATCHED', 'ACCEPTED', 'REJECTED', 'EN_ROUTE',
            'ARRIVED_SCENE', 'TRANSPORTING', 'ARRIVED_HOSPITAL',
            'COMPLETED', 'CANCELLED'
        ));

-- Dispatch resource
UPDATE public.dispatch_resources
SET status = CASE status
                 WHEN 'ACTIVE' THEN 'AVAILABLE'
                 WHEN 'BUSY' THEN 'ON_MISSION'
                 ELSE status
             END
WHERE status IN ('ACTIVE', 'BUSY');

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM public.dispatch_resources
        WHERE status IS NULL
           OR status NOT IN (
                'AVAILABLE', 'DISPATCHED', 'ON_MISSION', 'RETURNING',
                'OFFLINE', 'MAINTENANCE', 'OUT_OF_SERVICE'
           )
    ) THEN
        RAISE EXCEPTION 'dispatch_resources có status không được backend hỗ trợ';
    END IF;
END
$$;

ALTER TABLE public.dispatch_resources
    ALTER COLUMN status SET DEFAULT 'AVAILABLE',
    ALTER COLUMN status SET NOT NULL,
    DROP CONSTRAINT IF EXISTS dispatch_resources_status_check;

ALTER TABLE public.dispatch_resources
    ADD CONSTRAINT dispatch_resources_status_check
        CHECK (status IN (
            'AVAILABLE', 'DISPATCHED', 'ON_MISSION', 'RETURNING',
            'OFFLINE', 'MAINTENANCE', 'OUT_OF_SERVICE'
        ));

-- Ambulance simulation
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM public.ambulance_simulations
        WHERE status NOT IN ('READY', 'RUNNING', 'STOPPED', 'COMPLETED', 'FAILED', 'INTERRUPTED')
           OR phase NOT IN ('TO_SCENE', 'AT_SCENE', 'TO_HOSPITAL', 'ARRIVED_HOSPITAL')
    ) THEN
        RAISE EXCEPTION 'ambulance_simulations có status/phase không được backend hỗ trợ';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM public.simulation_legs
        WHERE leg_type NOT IN ('TO_SCENE', 'TO_HOSPITAL')
    ) THEN
        RAISE EXCEPTION 'simulation_legs có leg_type không được backend hỗ trợ';
    END IF;
END
$$;

ALTER TABLE public.ambulance_simulations
    DROP CONSTRAINT IF EXISTS ambulance_simulations_status_check,
    DROP CONSTRAINT IF EXISTS ambulance_simulations_phase_check;

ALTER TABLE public.ambulance_simulations
    ADD CONSTRAINT ambulance_simulations_status_check
        CHECK (status IN ('READY', 'RUNNING', 'STOPPED', 'COMPLETED', 'FAILED', 'INTERRUPTED')),
    ADD CONSTRAINT ambulance_simulations_phase_check
        CHECK (phase IN ('TO_SCENE', 'AT_SCENE', 'TO_HOSPITAL', 'ARRIVED_HOSPITAL'));

ALTER TABLE public.simulation_legs
    DROP CONSTRAINT IF EXISTS simulation_legs_leg_type_check;

ALTER TABLE public.simulation_legs
    ADD CONSTRAINT simulation_legs_leg_type_check
        CHECK (leg_type IN ('TO_SCENE', 'TO_HOSPITAL'));
