-- ============================================================
-- V9__seed_data.sql
-- Seed data hệ thống quản lý và điều phối cấp cứu
-- ============================================================

-- pass: 123456

-- ============================================================
-- 1. USERS
-- Thứ tự: ADMIN → DISPATCHER → DRIVER → USER → PROVIDER (owner)
-- Lưu ý: PROVIDER ở đây là role user đại diện cho nhà cung cấp
-- "Nhân viên y tế" dùng role USER (chưa có role MEDIC trong hệ thống)
-- ============================================================

INSERT INTO public.users (username, password_hash, full_name, phone_number, email, role, is_active)
VALUES
    -- 4 ADMIN
    ('admin01', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Nguyễn Văn An',      '0901000001', 'admin01@semd.vn', 'ADMIN', true),
    ('admin02', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Trần Thị Bình',      '0901000002', 'admin02@semd.vn', 'ADMIN', true),
    ('admin03', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Lê Văn Cường',       '0901000003', 'admin03@semd.vn', 'ADMIN', true),
    ('admin04', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Phạm Thị Dung',      '0901000004', 'admin04@semd.vn', 'ADMIN', true),

    -- 4 DISPATCHER
    ('dispatcher01', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Hoàng Văn Em',   '0902000001', 'dispatcher01@semd.vn', 'DISPATCHER', true),
    ('dispatcher02', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Ngô Thị Phương', '0902000002', 'dispatcher02@semd.vn', 'DISPATCHER', true),
    ('dispatcher03', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Đặng Văn Giang', '0902000003', 'dispatcher03@semd.vn', 'DISPATCHER', true),
    ('dispatcher04', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Vũ Thị Hoa',     '0902000004', 'dispatcher04@semd.vn', 'DISPATCHER', true),

    -- 4 DRIVER
    ('driver01', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Bùi Văn Inh',        '0903000001', 'driver01@semd.vn', 'DRIVER', true),
    ('driver02', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Đinh Thị Kim',       '0903000002', 'driver02@semd.vn', 'DRIVER', true),
    ('driver03', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Dương Văn Long',     '0903000003', 'driver03@semd.vn', 'DRIVER', true),
    ('driver04', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Lý Thị Mai',         '0903000004', 'driver04@semd.vn', 'DRIVER', true),

    -- 4 USER (người dân)
    ('user01', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Phan Văn Nam',         '0904000001', 'user01@gmail.com', 'USER', true),
    ('user02', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Tô Thị Oanh',          '0904000002', 'user02@gmail.com', 'USER', true),
    ('user03', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Hà Văn Phúc',          '0904000003', 'user03@gmail.com', 'USER', true),
    ('user04', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Cao Thị Quỳnh',        '0904000004', 'user04@gmail.com', 'USER', true),

    -- 4 Nhân viên y tế (dùng role USER — hệ thống chưa có role MEDIC)
    ('medic01', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Trịnh Văn Hùng',      '0905000001', 'medic01@semd.vn', 'USER', true),
    ('medic02', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Lưu Thị Sen',          '0905000002', 'medic02@semd.vn', 'USER', true),
    ('medic03', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Mai Văn Thắng',        '0905000003', 'medic03@semd.vn', 'USER', true),
    ('medic04', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Nguyễn Thị Uyên',     '0905000004', 'medic04@semd.vn', 'USER', true),

    -- 5 tài khoản đại diện Provider (role PROVIDER)
    ('provider01', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Trung tâm 115 Hoàn Kiếm',  '0906000001', 'provider01@semd.vn', 'PROVIDER', true),
    ('provider02', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Trung tâm 115 Đống Đa',    '0906000002', 'provider02@semd.vn', 'PROVIDER', true),
    ('provider03', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Trung tâm 115 Hai Bà Trưng','0906000003', 'provider03@semd.vn', 'PROVIDER', true),
    ('provider04', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Trung tâm 115 Cầu Giấy',   '0906000004', 'provider04@semd.vn', 'PROVIDER', true),
    ('provider05', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Trung tâm 115 Tây Hồ',     '0906000005', 'provider05@semd.vn', 'PROVIDER', true)
ON CONFLICT (username) DO NOTHING;


-- ============================================================
-- 2. PROVIDERS
-- Phụ thuộc vào users (owner_user_id)
-- provider_type: 'TRANSPORT' | 'CLINIC'
-- Dùng subquery để lấy id tránh hardcode
-- ============================================================

INSERT INTO public.providers (owner_user_id, provider_name, provider_type, business_license, contact_phone, contact_address, commission_rate, is_verified, is_active)
VALUES
    (
        (SELECT id FROM public.users WHERE username = 'provider01'),
        'Trung tâm Cấp cứu 115 Hoàn Kiếm', 'TRANSPORT',
        'GP-HK-2024-001', '0246000001', '15 Đinh Tiên Hoàng, Hoàn Kiếm, Hà Nội',
        10.00, true, true
    ),
    (
        (SELECT id FROM public.users WHERE username = 'provider02'),
        'Trung tâm Cấp cứu 115 Đống Đa', 'TRANSPORT',
        'GP-DD-2024-002', '0246000002', '87 Tôn Đức Thắng, Đống Đa, Hà Nội',
        10.00, true, true
    ),
    (
        (SELECT id FROM public.users WHERE username = 'provider03'),
        'Trung tâm Cấp cứu 115 Hai Bà Trưng', 'TRANSPORT',
        'GP-HBT-2024-003', '0246000003', '12 Bạch Mai, Hai Bà Trưng, Hà Nội',
        10.00, true, true
    ),
    (
        (SELECT id FROM public.users WHERE username = 'provider04'),
        'Trung tâm Cấp cứu 115 Cầu Giấy', 'TRANSPORT',
        'GP-CG-2024-004', '0246000004', '144 Xuân Thủy, Cầu Giấy, Hà Nội',
        10.00, true, true
    ),
    (
        (SELECT id FROM public.users WHERE username = 'provider05'),
        'Trung tâm Cấp cứu 115 Tây Hồ', 'TRANSPORT',
        'GP-TH-2024-005', '0246000005', '55 Âu Cơ, Tây Hồ, Hà Nội',
        10.00, true, true
    )
ON CONFLICT DO NOTHING;


-- ============================================================
-- 3. EDGE NODES — 4 vùng phủ tại Hà Nội (tọa độ ~21°N)
-- Mỗi vùng là một Polygon bao quanh 1 quận trung tâm
-- Thứ tự tọa độ: (lng lat) — PostGIS dùng (longitude, latitude)
-- ============================================================

INSERT INTO public.edge_nodes (node_name, coverage_area, is_active)
VALUES
    (
        'EdgeNode - Hoàn Kiếm',
        ST_GeomFromText(
            'POLYGON((105.845 21.020, 105.860 21.020, 105.860 21.035, 105.845 21.035, 105.845 21.020))',
            4326
        ),
        true
    ),
    (
        'EdgeNode - Đống Đa',
        ST_GeomFromText(
            'POLYGON((105.830 21.015, 105.845 21.015, 105.845 21.030, 105.830 21.030, 105.830 21.015))',
            4326
        ),
        true
    ),
    (
        'EdgeNode - Hai Bà Trưng',
        ST_GeomFromText(
            'POLYGON((105.855 21.005, 105.875 21.005, 105.875 21.025, 105.855 21.025, 105.855 21.005))',
            4326
        ),
        true
    ),
    (
        'EdgeNode - Cầu Giấy',
        ST_GeomFromText(
            'POLYGON((105.785 21.025, 105.810 21.025, 105.810 21.045, 105.785 21.045, 105.785 21.025))',
            4326
        ),
        true
    )
ON CONFLICT DO NOTHING;


-- ============================================================
-- 4. SERVICE TYPES — ALS và BLS
-- ALS: Advanced Life Support (hồi sức nâng cao)
-- BLS: Basic Life Support (hồi sức cơ bản)
-- ============================================================

INSERT INTO public.service_types (type_code, display_name, priority_weight)
VALUES
    ('ALS', 'Xe cấp cứu nâng cao (ALS)', 2),
    ('BLS', 'Xe cấp cứu cơ bản (BLS)',   1)
ON CONFLICT (type_code) DO NOTHING;


-- ============================================================
-- 5. DISPATCH RESOURCES — 5 xe cứu thương tại Hà Nội
-- Tọa độ nằm trong các vùng EdgeNode tương ứng
-- Xe 1-2: ALS, Xe 3-5: BLS
-- Driver gán lần lượt driver01 → driver04 (xe 5 chưa có tài xế)
-- ============================================================

INSERT INTO public.dispatch_resources
    (resource_code, resource_type_id, edge_node_id, provider_id, current_driver_id, status, current_location, extended_attributes)
VALUES
    (
        'AMB-HK-001',
        (SELECT id FROM public.service_types WHERE type_code = 'ALS'),
        (SELECT id FROM public.edge_nodes   WHERE node_name = 'EdgeNode - Hoàn Kiếm'),
        (SELECT id FROM public.providers    WHERE business_license = 'GP-HK-2024-001'),
        (SELECT id FROM public.users        WHERE username = 'driver01'),
        'AVAILABLE',
        ST_SetSRID(ST_MakePoint(105.852, 21.028), 4326),
        '{"plate": "29A-11111", "model": "Ford Transit", "year": 2022}'::jsonb
    ),
    (
        'AMB-DD-001',
        (SELECT id FROM public.service_types WHERE type_code = 'ALS'),
        (SELECT id FROM public.edge_nodes   WHERE node_name = 'EdgeNode - Đống Đa'),
        (SELECT id FROM public.providers    WHERE business_license = 'GP-DD-2024-002'),
        (SELECT id FROM public.users        WHERE username = 'driver02'),
        'AVAILABLE',
        ST_SetSRID(ST_MakePoint(105.838, 21.022), 4326),
        '{"plate": "29A-22222", "model": "Ford Transit", "year": 2021}'::jsonb
    ),
    (
        'AMB-HBT-001',
        (SELECT id FROM public.service_types WHERE type_code = 'BLS'),
        (SELECT id FROM public.edge_nodes   WHERE node_name = 'EdgeNode - Hai Bà Trưng'),
        (SELECT id FROM public.providers    WHERE business_license = 'GP-HBT-2024-003'),
        (SELECT id FROM public.users        WHERE username = 'driver03'),
        'AVAILABLE',
        ST_SetSRID(ST_MakePoint(105.865, 21.015), 4326),
        '{"plate": "29A-33333", "model": "Hyundai Starex", "year": 2023}'::jsonb
    ),
    (
        'AMB-CG-001',
        (SELECT id FROM public.service_types WHERE type_code = 'BLS'),
        (SELECT id FROM public.edge_nodes   WHERE node_name = 'EdgeNode - Cầu Giấy'),
        (SELECT id FROM public.providers    WHERE business_license = 'GP-CG-2024-004'),
        (SELECT id FROM public.users        WHERE username = 'driver04'),
        'AVAILABLE',
        ST_SetSRID(ST_MakePoint(105.795, 21.035), 4326),
        '{"plate": "29A-44444", "model": "Toyota HiAce", "year": 2022}'::jsonb
    ),
    (
        'AMB-TH-001',
        (SELECT id FROM public.service_types WHERE type_code = 'BLS'),
        (SELECT id FROM public.edge_nodes   WHERE node_name = 'EdgeNode - Cầu Giấy'),
        (SELECT id FROM public.providers    WHERE business_license = 'GP-TH-2024-005'),
        NULL, -- chưa gán tài xế
        'AVAILABLE',
        ST_SetSRID(ST_MakePoint(105.800, 21.038), 4326),
        '{"plate": "29A-55555", "model": "Hyundai Starex", "year": 2020}'::jsonb
    )
ON CONFLICT (resource_code) DO NOTHING;
