-- =====================================================================
-- V3__emergency_and_resources.sql
-- emergency_calls, dispatch_resources (đã gắn provider_id)
-- =====================================================================

-- 1. Table: emergency_calls
CREATE TABLE public.emergency_calls
(
    id                    integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    dispatcher_id         integer REFERENCES public.users(id) ON DELETE SET NULL,
    reporter_phone        varchar(15) NOT NULL,
    reporter_name         varchar(100),
    call_start_time       timestamp NOT NULL,
    call_end_time         timestamp,
    audio_url             varchar(512),
    ai_transcript         text,
    ai_urgency_prediction varchar(20),
    ai_confidence_score   numeric(5,2),
    status                varchar(20) DEFAULT 'RECEIVED',
    created_at            timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_calls_status ON public.emergency_calls(status);

-- 2. Table: dispatch_resources (Tài nguyên: xe cấp cứu, đội y tế, ...)
-- provider_id: đơn vị sở hữu (Nhà xe / Phòng khám) - cốt lõi để đối soát công nợ
-- current_driver_id: tài xế hiện đang vận hành resource
CREATE TABLE public.dispatch_resources
(
    id                  integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    resource_code       varchar(50) NOT NULL UNIQUE,
    resource_type_id    integer REFERENCES public.service_types(id) ON DELETE SET NULL,
    edge_node_id        integer REFERENCES public.edge_nodes(id) ON DELETE SET NULL,
    provider_id         integer REFERENCES public.providers(id) ON DELETE SET NULL,
    current_driver_id   integer REFERENCES public.users(id) ON DELETE SET NULL,
    status              varchar(20) DEFAULT 'AVAILABLE',
    current_location    geography(Point,4326),
    extended_attributes jsonb,
    updated_at          timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_resources_provider ON public.dispatch_resources(provider_id);
CREATE INDEX idx_resources_driver ON public.dispatch_resources(current_driver_id);
CREATE INDEX idx_resources_attributes_gin
    ON public.dispatch_resources USING gin (extended_attributes);
CREATE INDEX idx_resources_location
    ON public.dispatch_resources USING gist (current_location);

CREATE TRIGGER update_resources_modtime
    BEFORE UPDATE ON public.dispatch_resources
    FOR EACH ROW
    EXECUTE FUNCTION public.update_modified_column();
