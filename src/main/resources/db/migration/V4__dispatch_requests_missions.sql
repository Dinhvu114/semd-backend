-- =====================================================================
-- V4__dispatch_requests_missions.sql
-- dispatch_requests, dispatch_missions
-- =====================================================================

-- 1. Table: dispatch_requests (Yêu cầu điều phối)
CREATE TABLE public.dispatch_requests
(
    id                       integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    call_id                  integer REFERENCES public.emergency_calls(id) ON DELETE SET NULL,
    service_type_id          integer REFERENCES public.service_types(id) ON DELETE SET NULL,
    edge_node_id             integer REFERENCES public.edge_nodes(id) ON DELETE SET NULL,
    created_by_dispatcher_id integer REFERENCES public.users(id) ON DELETE SET NULL,
    urgency_level            varchar(20) DEFAULT 'MEDIUM',
    target_location          geography(Point,4326) NOT NULL,
    status                   varchar(20) DEFAULT 'PENDING',
    extended_requirements    jsonb,
    is_synced_to_cloud       boolean DEFAULT false,
    created_at               timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_requests_location
    ON public.dispatch_requests USING gist (target_location);
CREATE INDEX idx_requests_requirements_gin
    ON public.dispatch_requests USING gin (extended_requirements);
CREATE INDEX idx_requests_status ON public.dispatch_requests(status);

-- 2. Table: dispatch_missions (Nhiệm vụ điều phối thực tế)
CREATE TABLE public.dispatch_missions
(
    id                integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    request_id        integer NOT NULL UNIQUE REFERENCES public.dispatch_requests(id) ON DELETE RESTRICT,
    resource_id       integer NOT NULL REFERENCES public.dispatch_resources(id) ON DELETE RESTRICT,
    destination_id    integer REFERENCES public.medical_hospitals(id) ON DELETE SET NULL,
    destination_name  varchar(255),
    status            varchar(20) DEFAULT 'DISPATCHED',
    dispatched_at     timestamp DEFAULT CURRENT_TIMESTAMP,
    accepted_at       timestamp,
    en_route_at       timestamp,
    on_scene_at       timestamp,
    completed_at      timestamp,
    route_geometry    geography(LineString,4326)
);

CREATE INDEX idx_missions_resource ON public.dispatch_missions(resource_id);
CREATE INDEX idx_missions_destination ON public.dispatch_missions(destination_id);
CREATE INDEX idx_missions_status ON public.dispatch_missions(status);
