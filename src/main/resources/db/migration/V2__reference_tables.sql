-- =====================================================================
-- V2__reference_tables.sql
-- Các bảng tham chiếu: edge_nodes, service_types, medical_hospitals
-- =====================================================================

-- 1. Table: edge_nodes (khu vực phụ trách / node biên)
CREATE TABLE public.edge_nodes
(
    id            integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    node_name     varchar(100) NOT NULL,
    coverage_area geometry(Polygon,4326),
    is_active     boolean DEFAULT true,
    created_at    timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_edge_nodes_coverage
    ON public.edge_nodes USING gist (coverage_area);

-- 2. Table: service_types (loại dịch vụ: xe cấp cứu, taxi y tế, ...)
CREATE TABLE public.service_types
(
    id              integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type_code       varchar(50)  NOT NULL UNIQUE,
    display_name    varchar(100) NOT NULL,
    priority_weight integer DEFAULT 1
);

-- 3. Table: medical_hospitals (Bệnh viện)
-- owner_user_id: tài khoản đại diện bệnh viện đăng nhập Web Portal (role = HOSPITAL)
CREATE TABLE public.medical_hospitals
(
    id                integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    owner_user_id     integer REFERENCES public.users(id) ON DELETE SET NULL,
    hospital_name     varchar(150) NOT NULL,
    hospital_address  varchar(255),
    location          geography(Point,4326) NOT NULL,
    capabilities      jsonb,           -- danh sách chuyên khoa, số giường ER, ...
    contact_phone     varchar(15),
    is_active         boolean DEFAULT true
);

CREATE INDEX idx_hospitals_owner ON public.medical_hospitals(owner_user_id);
CREATE INDEX idx_hospitals_capabilities_gin
    ON public.medical_hospitals USING gin (capabilities);
CREATE INDEX idx_hospitals_location
    ON public.medical_hospitals USING gist (location);
