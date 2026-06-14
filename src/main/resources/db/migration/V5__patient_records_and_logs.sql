-- =====================================================================
-- V5__patient_records_and_logs.sql
-- emergency_patient_records, resource_location_logs
-- =====================================================================

-- 1. Table: emergency_patient_records (Hồ sơ bệnh nhân cấp cứu)
CREATE TABLE public.emergency_patient_records
(
    id            integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    mission_id    integer NOT NULL REFERENCES public.dispatch_missions(id) ON DELETE CASCADE,
    patient_id    integer REFERENCES public.users(id) ON DELETE SET NULL,
    patient_name  varchar(100) DEFAULT 'Chưa rõ danh tính',
    gender        varchar(10),
    age           integer,
    triage_color  varchar(10) DEFAULT 'YELLOW',
    vital_signs   jsonb,
    clinical_note text,
    updated_at    timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_patient_records_patient_id
    ON public.emergency_patient_records(patient_id);
CREATE INDEX idx_patient_records_mission_id
    ON public.emergency_patient_records(mission_id);

CREATE TRIGGER update_patient_records_modtime
    BEFORE UPDATE ON public.emergency_patient_records
    FOR EACH ROW
    EXECUTE FUNCTION public.update_modified_column();

-- 2. Table: resource_location_logs (Lịch sử vị trí xe - GPS tracking)
CREATE TABLE public.resource_location_logs
(
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    resource_id integer REFERENCES public.dispatch_resources(id) ON DELETE CASCADE,
    location    geography(Point,4326) NOT NULL,
    speed       numeric(5,2),
    recorded_at timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_location_logs_geo
    ON public.resource_location_logs USING gist (location);
CREATE INDEX idx_location_logs_resource_time
    ON public.resource_location_logs(resource_id, recorded_at DESC);
