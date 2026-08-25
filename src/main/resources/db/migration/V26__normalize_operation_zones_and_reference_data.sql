-- Chuẩn hóa toàn bộ tên cột/constraint/index còn mang tên edge node sang operation zone.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'dispatch_requests'
          AND column_name = 'edge_node_id'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'dispatch_requests'
          AND column_name = 'zone_id'
    ) THEN
        ALTER TABLE public.dispatch_requests RENAME COLUMN edge_node_id TO zone_id;
    END IF;
END
$$;



ALTER INDEX public.idx_edge_nodes_coverage RENAME TO idx_operation_zones_coverage;

ALTER TABLE public.dispatch_resources
    RENAME CONSTRAINT dispatch_resources_edge_node_id_fkey
        TO dispatch_resources_zone_id_fkey;

ALTER TABLE public.dispatch_requests
    RENAME CONSTRAINT dispatch_requests_edge_node_id_fkey
        TO dispatch_requests_zone_id_fkey;

CREATE INDEX IF NOT EXISTS idx_dispatch_resources_zone
    ON public.dispatch_resources(zone_id);
CREATE INDEX IF NOT EXISTS idx_dispatch_requests_zone
    ON public.dispatch_requests(zone_id);

UPDATE public.operation_zones
SET zone_name = regexp_replace(zone_name, '^EdgeNode\s*-\s*', 'Vùng ')
WHERE zone_name ~ '^EdgeNode\s*-\s*';

-- Chuẩn hóa tên người dùng theo dữ liệu đã xác nhận.
UPDATE public.users
SET full_name = 'Bùi Văn Linh'
WHERE full_name = 'Bùi Văn Inh';

UPDATE public.users
SET full_name = 'Hoàng Văn Ánh'
WHERE full_name = 'Hoàng Văn Em';

-- Dữ liệu bệnh viện Hà Nội. Tọa độ dùng SRID 4326, X = kinh độ, Y = vĩ độ.
INSERT INTO public.medical_hospitals
    (hospital_name, hospital_address, location, capabilities, contact_phone, is_active)
SELECT
    'Bệnh viện Bạch Mai',
    '78 Giải Phóng, phường Kim Liên, Hà Nội',
    ST_SetSRID(ST_MakePoint(105.8399, 21.0028), 4326)::geography,
    '{"level":"CENTRAL","emergency_24_7":true,"specialties":["INTERNAL_MEDICINE","CARDIOLOGY","NEUROLOGY","INTENSIVE_CARE"]}'::jsonb,
    '02438689711',
    TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM public.medical_hospitals
    WHERE lower(hospital_name) = lower('Bệnh viện Bạch Mai')
);

INSERT INTO public.medical_hospitals
    (hospital_name, hospital_address, location, capabilities, contact_phone, is_active)
SELECT
    'Bệnh viện Hữu nghị Việt Đức',
    '40 Tràng Thi, phường Hoàn Kiếm, Hà Nội',
    ST_SetSRID(ST_MakePoint(105.8467, 21.0284), 4326)::geography,
    '{"level":"CENTRAL","emergency_24_7":true,"specialties":["TRAUMA","SURGERY","NEUROSURGERY","ORTHOPEDICS"]}'::jsonb,
    '02438253531',
    TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM public.medical_hospitals
    WHERE lower(hospital_name) IN (
        lower('Bệnh viện Hữu nghị Việt Đức'),
        lower('Bệnh viện Việt Đức')
    )
);

INSERT INTO public.medical_hospitals
    (hospital_name, hospital_address, location, capabilities, contact_phone, is_active)
SELECT
    'Bệnh viện Quân y 103',
    '261 Phùng Hưng, phường Hà Đông, Hà Nội',
    ST_SetSRID(ST_MakePoint(105.7876, 20.9669), 4326)::geography,
    '{"level":"CENTRAL_MILITARY","emergency_24_7":true,"specialties":["GENERAL_MEDICINE","TRAUMA","SURGERY","CARDIOLOGY"]}'::jsonb,
    '0967811616',
    TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM public.medical_hospitals
    WHERE lower(hospital_name) IN (
        lower('Bệnh viện Quân y 103'),
        lower('Bệnh viện 103')
    )
);

INSERT INTO public.medical_hospitals
    (hospital_name, hospital_address, location, capabilities, contact_phone, is_active)
SELECT
    'Bệnh viện Trung ương Quân đội 108',
    '1 Trần Hưng Đạo, phường Hai Bà Trưng, Hà Nội',
    ST_SetSRID(ST_MakePoint(105.8605, 21.0180), 4326)::geography,
    '{"level":"CENTRAL_MILITARY","emergency_24_7":true,"specialties":["GENERAL_MEDICINE","TRAUMA","SURGERY","INTENSIVE_CARE"]}'::jsonb,
    '0695724000',
    TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM public.medical_hospitals
    WHERE lower(hospital_name) IN (
        lower('Bệnh viện Trung ương Quân đội 108'),
        lower('Bệnh viện 108')
    )
);

INSERT INTO public.medical_hospitals
    (hospital_name, hospital_address, location, capabilities, contact_phone, is_active)
SELECT
    'Bệnh viện Đa khoa Tâm Anh Hà Nội',
    '108 Hoàng Như Tiếp, phường Bồ Đề, Hà Nội',
    ST_SetSRID(ST_MakePoint(105.8764, 21.0409), 4326)::geography,
    '{"level":"PRIVATE_GENERAL","emergency_24_7":true,"specialties":["GENERAL_MEDICINE","CARDIOLOGY","PEDIATRICS","OBSTETRICS"]}'::jsonb,
    '02471066858',
    TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM public.medical_hospitals
    WHERE lower(hospital_name) IN (
        lower('Bệnh viện Đa khoa Tâm Anh Hà Nội'),
        lower('Bệnh viện Tâm Anh')
    )
);
