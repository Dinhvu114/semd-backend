-- ==========================================
-- SEED DATA — Dữ liệu mẫu để demo/test
-- Chỉ insert nếu chưa có dữ liệu
-- ==========================================

-- 1. Users (password = SHA256 của 'Admin@123')
INSERT INTO users (username, password_hash, full_name, phone_number, role, is_active)
SELECT 'admin', 'b7e74a79b9e8f8d9c1e2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4', 'Quản trị viên', '0900000001', 'ADMIN', true
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

INSERT INTO users (username, password_hash, full_name, phone_number, role, is_active)
SELECT 'dispatcher01', 'b7e74a79b9e8f8d9c1e2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4', 'Nguyễn Điều Phối', '0900000002', 'DISPATCHER', true
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'dispatcher01');

INSERT INTO users (username, password_hash, full_name, phone_number, role, is_active)
SELECT 'driver01', 'b7e74a79b9e8f8d9c1e2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4', 'Trần Tài Xế', '0900000003', 'DRIVER', true
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'driver01');

INSERT INTO users (username, password_hash, full_name, phone_number, role, is_active)
SELECT 'reporter01', 'b7e74a79b9e8f8d9c1e2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4', 'Lê Người Dùng', '0900000004', 'REPORTER', true
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'reporter01');

INSERT INTO users (username, password_hash, full_name, phone_number, role, is_active)
SELECT 'provider01', 'b7e74a79b9e8f8d9c1e2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4', 'Phạm Nhà Cung Cấp', '0900000005', 'PROVIDER', true
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'provider01');

-- 2. Service Types
INSERT INTO service_types (type_code, display_name, priority_weight)
SELECT 'MED_ACCIDENT', 'Tai nạn giao thông', 3
    WHERE NOT EXISTS (SELECT 1 FROM service_types WHERE type_code = 'MED_ACCIDENT');

INSERT INTO service_types (type_code, display_name, priority_weight)
SELECT 'MED_STROKE', 'Đột quỵ', 5
    WHERE NOT EXISTS (SELECT 1 FROM service_types WHERE type_code = 'MED_STROKE');

INSERT INTO service_types (type_code, display_name, priority_weight)
SELECT 'MED_CARDIAC', 'Tim mạch', 5
    WHERE NOT EXISTS (SELECT 1 FROM service_types WHERE type_code = 'MED_CARDIAC');

INSERT INTO service_types (type_code, display_name, priority_weight)
SELECT 'MED_GENERAL', 'Cấp cứu thông thường', 1
    WHERE NOT EXISTS (SELECT 1 FROM service_types WHERE type_code = 'MED_GENERAL');

-- 3. Medical Hospitals (Hà Nội)
INSERT INTO medical_hospitals (hospital_name, hospital_address, location, contact_phone, is_active)
SELECT
    'Bệnh viện Bạch Mai',
    '78 Giải Phóng, Phương Mai, Đống Đa, Hà Nội',
    ST_GeogFromText('SRID=4326;POINT(105.8412 21.0008)'),
    '02438694231',
    true
    WHERE NOT EXISTS (SELECT 1 FROM medical_hospitals WHERE hospital_name = 'Bệnh viện Bạch Mai');

INSERT INTO medical_hospitals (hospital_name, hospital_address, location, contact_phone, is_active)
SELECT
    'Bệnh viện Việt Đức',
    '40 Tràng Thi, Hoàn Kiếm, Hà Nội',
    ST_GeogFromText('SRID=4326;POINT(105.8484 21.0285)'),
    '02438253531',
    true
    WHERE NOT EXISTS (SELECT 1 FROM medical_hospitals WHERE hospital_name = 'Bệnh viện Việt Đức');

INSERT INTO medical_hospitals (hospital_name, hospital_address, location, contact_phone, is_active)
SELECT
    'Bệnh viện 108',
    '1 Trần Hưng Đạo, Hai Bà Trưng, Hà Nội',
    ST_GeogFromText('SRID=4326;POINT(105.8534 21.0195)'),
    '02469748484',
    true
    WHERE NOT EXISTS (SELECT 1 FROM medical_hospitals WHERE hospital_name = 'Bệnh viện 108');

-- 4. Operation Zones (nếu có bảng này)
INSERT INTO operation_zones (zone_name, coverage_area, is_active)
SELECT
    'Vùng Trung Tâm Hà Nội',
    ST_GeogFromText('SRID=4326;POLYGON((105.80 21.05, 105.88 21.05, 105.88 20.99, 105.80 20.99, 105.80 21.05))'),
    true
    WHERE NOT EXISTS (SELECT 1 FROM operation_zones WHERE zone_name = 'Vùng Trung Tâm Hà Nội')
  AND EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'operation_zones');

-- 5. Dispatch Resources (xe cứu thương)
INSERT INTO dispatch_resources (resource_code, status, current_location, current_driver_id, updated_at)
SELECT
    '29A-11111',
    'AVAILABLE',
    ST_GeogFromText('SRID=4326;POINT(105.8342 21.0278)'),
    (SELECT id FROM users WHERE username = 'driver01' LIMIT 1),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM dispatch_resources WHERE resource_code = '29A-11111');

INSERT INTO dispatch_resources (resource_code, status, current_location, updated_at)
SELECT
    '29A-22222',
    'AVAILABLE',
    ST_GeogFromText('SRID=4326;POINT(105.8501 21.0322)'),
    NOW()
    WHERE NOT EXISTS (SELECT 1 FROM dispatch_resources WHERE resource_code = '29A-22222');

INSERT INTO dispatch_resources (resource_code, status, current_location, updated_at)
SELECT
    '29A-33333',
    'AVAILABLE',
    ST_GeogFromText('SRID=4326;POINT(105.8200 21.0150)'),
    NOW()
    WHERE NOT EXISTS (SELECT 1 FROM dispatch_resources WHERE resource_code = '29A-33333');