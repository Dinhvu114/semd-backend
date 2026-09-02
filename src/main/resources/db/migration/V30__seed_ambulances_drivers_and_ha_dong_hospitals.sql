-- Seed additional ambulance drivers, ambulances and medical facilities in western Ha Noi.
-- Password for seeded driver accounts: 123456 (same SHA-256 hash used by V9).

-- Rename the two existing demo drivers requested by the product owner.
UPDATE public.users
SET full_name = 'Nguyễn Minh Quân'
WHERE username = 'driver02';

UPDATE public.users
SET full_name = 'Trần Quốc Huy'
WHERE username = 'driver04';

-- Add one driver for the existing unassigned ambulance and three drivers for new ambulances.
INSERT INTO public.users
    (username, password_hash, full_name, phone_number, email, is_active, provider_id)
VALUES
    (
        'driver05',
        '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92',
        'Phạm Đức Thành',
        '0903000005',
        'driver05@semd.vn',
        TRUE,
        (SELECT id FROM public.providers WHERE business_license = 'GP-TH-2024-005')
    ),
    (
        'driver06',
        '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92',
        'Nguyễn Tuấn Kiệt',
        '0903000006',
        'driver06@semd.vn',
        TRUE,
        (SELECT id FROM public.providers WHERE business_license = 'GP-HK-2024-001')
    ),
    (
        'driver07',
        '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92',
        'Lê Hoàng Nam',
        '0903000007',
        'driver07@semd.vn',
        TRUE,
        (SELECT id FROM public.providers WHERE business_license = 'GP-DD-2024-002')
    ),
    (
        'driver08',
        '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92',
        'Đỗ Văn Khánh',
        '0903000008',
        'driver08@semd.vn',
        TRUE,
        (SELECT id FROM public.providers WHERE business_license = 'GP-HBT-2024-003')
    )
ON CONFLICT (username) DO NOTHING;

-- Ensure every newly seeded account has the DRIVER role in the RBAC model.
INSERT INTO public.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM public.users u
CROSS JOIN public.roles r
WHERE u.username IN ('driver05', 'driver06', 'driver07', 'driver08')
  AND r.name = 'DRIVER'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- Assign a dedicated driver to the ambulance intentionally left unassigned by V9.
UPDATE public.dispatch_resources
SET current_driver_id = (SELECT id FROM public.users WHERE username = 'driver05')
WHERE resource_code = 'AMB-TH-001'
  AND current_driver_id IS NULL;

-- Add three ambulances covering Ha Dong, Duong Noi and Yen Nghia.
INSERT INTO public.dispatch_resources
    (resource_code, resource_type_id, provider_id, current_driver_id, status,
     current_location, extended_attributes)
VALUES
    (
        'AMB-HD-001',
        (SELECT id FROM public.service_types WHERE type_code = 'ALS'),
        (SELECT id FROM public.providers WHERE business_license = 'GP-HK-2024-001'),
        (SELECT id FROM public.users WHERE username = 'driver06'),
        'AVAILABLE',
        ST_SetSRID(ST_MakePoint(105.7763, 20.9712), 4326)::geography,
        '{"plate":"30G-610.01","model":"Ford Transit","year":2024,"baseWard":"Hà Đông"}'::jsonb
    ),
    (
        'AMB-DN-001',
        (SELECT id FROM public.service_types WHERE type_code = 'BLS'),
        (SELECT id FROM public.providers WHERE business_license = 'GP-DD-2024-002'),
        (SELECT id FROM public.users WHERE username = 'driver07'),
        'AVAILABLE',
        ST_SetSRID(ST_MakePoint(105.7518, 20.9973), 4326)::geography,
        '{"plate":"30G-610.02","model":"Hyundai Solati","year":2023,"baseWard":"Dương Nội"}'::jsonb
    ),
    (
        'AMB-YN-001',
        (SELECT id FROM public.service_types WHERE type_code = 'BLS'),
        (SELECT id FROM public.providers WHERE business_license = 'GP-HBT-2024-003'),
        (SELECT id FROM public.users WHERE username = 'driver08'),
        'AVAILABLE',
        ST_SetSRID(ST_MakePoint(105.7359, 20.9468), 4326)::geography,
        '{"plate":"30G-610.03","model":"Toyota HiAce","year":2024,"baseWard":"Yên Nghĩa"}'::jsonb
    )
ON CONFLICT (resource_code) DO NOTHING;

-- Two medical facilities in Ha Dong ward.
INSERT INTO public.medical_hospitals
    (hospital_name, hospital_address, location, capabilities, contact_phone, is_active)
SELECT
    'Bệnh viện Đa khoa Hà Đông',
    '2 Bế Văn Đàn, phường Hà Đông, Hà Nội',
    ST_SetSRID(ST_MakePoint(105.7763, 20.9712), 4326)::geography,
    '{"level":"GENERAL","emergency_24_7":true,"ward":"Hà Đông","specialties":["EMERGENCY","GENERAL_MEDICINE","SURGERY","PEDIATRICS"]}'::jsonb,
    '02433822267',
    TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM public.medical_hospitals
    WHERE lower(hospital_name) = lower('Bệnh viện Đa khoa Hà Đông')
);

INSERT INTO public.medical_hospitals
    (hospital_name, hospital_address, location, capabilities, contact_phone, is_active)
SELECT
    'Phòng khám Đa khoa SmartCare Hà Đông',
    '68 Quang Trung, phường Hà Đông, Hà Nội',
    ST_SetSRID(ST_MakePoint(105.7744, 20.9735), 4326)::geography,
    '{"level":"CLINIC","emergency_24_7":false,"ward":"Hà Đông","specialties":["GENERAL_MEDICINE","PEDIATRICS","DIAGNOSTIC_IMAGING"]}'::jsonb,
    '02473001001',
    TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM public.medical_hospitals
    WHERE lower(hospital_name) = lower('Phòng khám Đa khoa SmartCare Hà Đông')
);

-- Two medical facilities in Duong Noi ward.
INSERT INTO public.medical_hospitals
    (hospital_name, hospital_address, location, capabilities, contact_phone, is_active)
SELECT
    'Phòng khám Đa khoa Dương Nội',
    'Khu đô thị Dương Nội, phường Dương Nội, Hà Nội',
    ST_SetSRID(ST_MakePoint(105.7518, 20.9973), 4326)::geography,
    '{"level":"CLINIC","emergency_24_7":true,"ward":"Dương Nội","specialties":["EMERGENCY","GENERAL_MEDICINE","PEDIATRICS"]}'::jsonb,
    '02473001002',
    TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM public.medical_hospitals
    WHERE lower(hospital_name) = lower('Phòng khám Đa khoa Dương Nội')
);

INSERT INTO public.medical_hospitals
    (hospital_name, hospital_address, location, capabilities, contact_phone, is_active)
SELECT
    'Trung tâm Y tế Dương Nội',
    'Đường Tố Hữu, phường Dương Nội, Hà Nội',
    ST_SetSRID(ST_MakePoint(105.7549, 20.9948), 4326)::geography,
    '{"level":"MEDICAL_CENTER","emergency_24_7":false,"ward":"Dương Nội","specialties":["GENERAL_MEDICINE","INTERNAL_MEDICINE","OBSTETRICS"]}'::jsonb,
    '02473001003',
    TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM public.medical_hospitals
    WHERE lower(hospital_name) = lower('Trung tâm Y tế Dương Nội')
);

-- Two medical facilities in Yen Nghia ward.
INSERT INTO public.medical_hospitals
    (hospital_name, hospital_address, location, capabilities, contact_phone, is_active)
SELECT
    'Bệnh viện Nhi Hà Nội',
    'Khu đô thị mới, phường Yên Nghĩa, Hà Nội',
    ST_SetSRID(ST_MakePoint(105.7359, 20.9468), 4326)::geography,
    '{"level":"SPECIALIZED","emergency_24_7":true,"ward":"Yên Nghĩa","specialties":["PEDIATRICS","PEDIATRIC_EMERGENCY","INTENSIVE_CARE"]}'::jsonb,
    '02473001004',
    TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM public.medical_hospitals
    WHERE lower(hospital_name) = lower('Bệnh viện Nhi Hà Nội')
);

INSERT INTO public.medical_hospitals
    (hospital_name, hospital_address, location, capabilities, contact_phone, is_active)
SELECT
    'Phòng khám Đa khoa Yên Nghĩa',
    'Đường Yên Nghĩa, phường Yên Nghĩa, Hà Nội',
    ST_SetSRID(ST_MakePoint(105.7332, 20.9504), 4326)::geography,
    '{"level":"CLINIC","emergency_24_7":false,"ward":"Yên Nghĩa","specialties":["GENERAL_MEDICINE","INTERNAL_MEDICINE","DIAGNOSTIC_IMAGING"]}'::jsonb,
    '02473001005',
    TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM public.medical_hospitals
    WHERE lower(hospital_name) = lower('Phòng khám Đa khoa Yên Nghĩa')
);

-- Fail the migration if any active ambulance is still missing a driver.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM public.dispatch_resources
        WHERE current_driver_id IS NULL
          AND status <> 'MAINTENANCE'
    ) THEN
        RAISE EXCEPTION 'Active dispatch resource without an assigned driver remains after V29';
    END IF;
END
$$;
