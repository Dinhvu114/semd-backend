-- ==================================================================================
-- HỆ THỐNG ĐIỀU PHỐI Y TẾ NGOẠI VIỆN
-- Phiên bản: 1.0
-- ==================================================================================

-- 0. KÍCH HOẠT TIỆN ÍCH MỞ RỘNG KHÔNG GIAN
CREATE EXTENSION IF NOT EXISTS postgis;

-- ==================================================================================
-- NHÓM 1: CÁC BẢNG DANH MỤC VÀ QUẢN LÝ TÀI KHOẢN (Độc lập, không chứa khóa ngoại)
-- ==================================================================================

-- 1. Bảng edge_nodes (Trạm/Nút hạ tầng quản lý vùng)
CREATE TABLE edge_nodes (
                            id SERIAL PRIMARY KEY,
                            node_name VARCHAR(100) NOT NULL,
                            coverage_area GEOMETRY(Polygon, 4326), -- Vùng phủ sóng quản lý cục bộ
                            is_active BOOLEAN DEFAULT TRUE,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Bảng service_types (Danh mục loại dịch vụ cấp cứu/vận chuyển)
CREATE TABLE service_types (
                               id SERIAL PRIMARY KEY,
                               type_code VARCHAR(50) UNIQUE NOT NULL, -- Ví dụ: 'MED_ACCIDENT', 'MED_STROKE'
                               display_name VARCHAR(100) NOT NULL,
                               priority_weight INT DEFAULT 1
);

-- 3. Bảng medical_hospitals (Danh mục bệnh viện tiếp nhận bệnh nhân)
CREATE TABLE medical_hospitals (
                                   id SERIAL PRIMARY KEY,
                                   hospital_name VARCHAR(150) NOT NULL,
                                   hospital_address VARCHAR(255),
                                   location GEOGRAPHY(Point, 4326) NOT NULL, -- Tọa độ phục vụ thuật toán tìm trạm gần nhất
                                   capabilities JSONB, -- Năng lực y tế trực ca (Số giường trống, chuyên khoa...)
                                   contact_phone VARCHAR(15),
                                   is_active BOOLEAN DEFAULT TRUE
);

-- 4. Bảng users (Quản lý tài khoản: Dispatcher, Driver, Bác sĩ...)
CREATE TABLE users (
                       id SERIAL PRIMARY KEY,
                       username VARCHAR(50) UNIQUE NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       full_name VARCHAR(100) NOT NULL,
                       phone_number VARCHAR(15) UNIQUE NOT NULL,
                       role VARCHAR(20) NOT NULL, -- 'DISPATCHER', 'DRIVER', 'ADMIN', 'MEDICAL_STAFF'
                       is_active BOOLEAN DEFAULT TRUE,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==================================================================================
-- NHÓM 2: CÁC BẢNG LIÊN QUAN ĐẾN CUỘC GỌI VÀ TÀI NGUYÊN VẬN HÀNH
-- ==================================================================================

-- 5. Bảng emergency_calls (Log cuộc gọi tổng đài & Tích hợp cục bộ AI Speech-To-Text)
CREATE TABLE emergency_calls (
                                 id SERIAL PRIMARY KEY,
                                 dispatcher_id INT REFERENCES users(id) ON DELETE SET NULL, -- Tiếp nhận viên trực trạm
                                 reporter_phone VARCHAR(15) NOT NULL,
                                 reporter_name VARCHAR(100),
                                 call_start_time TIMESTAMP NOT NULL,
                                 call_end_time TIMESTAMP,
                                 audio_url VARCHAR(512), -- Link file lưu local tại trạm để re-train AI

    -- Trục dữ liệu AI bóc tách giọng nói Offline tại Biên (Vosk/Whisper)
                                 ai_transcript TEXT,
                                 ai_urgency_prediction VARCHAR(20), -- Gợi ý mức độ: LOW, MEDIUM, HIGH, CRITICAL
                                 ai_confidence_score NUMERIC(5,2),

                                 status VARCHAR(20) DEFAULT 'RECEIVED', -- RECEIVED, PROCESSING, CREATED_REQUEST, SPAM
                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 6. Bảng dispatch_resources (Phương tiện/Xe cứu thương cứu hộ)
CREATE TABLE dispatch_resources (
                                    id SERIAL PRIMARY KEY,
                                    resource_code VARCHAR(50) UNIQUE NOT NULL, -- Biển số xe hoặc mã định danh
                                    resource_type_id INT REFERENCES service_types(id) ON DELETE SET NULL,
                                    edge_node_id INT REFERENCES edge_nodes(id) ON DELETE SET NULL,
                                    current_driver_id INT REFERENCES users(id) ON DELETE SET NULL, -- Tài xế nhận ca trực
                                    status VARCHAR(20) DEFAULT 'AVAILABLE', -- AVAILABLE, ON_MISSION, OFFLINE
                                    current_location GEOGRAPHY(Point, 4326), -- Vị trí cập nhật tức thời để quét nhanh
                                    extended_attributes JSONB, -- Thuộc tính động (Kíp trực y tế gồm những ai, cơ số thuốc...)
                                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==================================================================================
-- NHÓM 3: TIẾN TRÌNH ĐIỀU PHỐI, CHUYẾN ĐI VÀ LÂM SÀNG (Luồng dữ liệu chính)
-- ==================================================================================

-- 7. Bảng dispatch_requests (Yêu cầu cứu hộ được phê duyệt từ cuộc gọi)
CREATE TABLE dispatch_requests (
                                   id SERIAL PRIMARY KEY,
                                   call_id INT REFERENCES emergency_calls(id) ON DELETE SET NULL, -- Tham chiếu tới cuộc gọi gốc
                                   created_by_dispatcher_id INT REFERENCES users(id) ON DELETE SET NULL,
                                   service_type_id INT REFERENCES service_types(id) ON DELETE SET NULL,
                                   edge_node_id INT REFERENCES edge_nodes(id) ON DELETE SET NULL,
                                   urgency_level VARCHAR(20) DEFAULT 'MEDIUM', -- LOW, MEDIUM, HIGH, CRITICAL
                                   target_location GEOGRAPHY(Point, 4326) NOT NULL, -- Nơi xảy ra tai nạn/thảm họa
                                   status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, ALLOCATING, DISPATCHED, COMPLETED, CANCELLED
                                   extended_requirements JSONB, -- Triệu chứng lâm sàng sơ bộ, ghi chú bổ sung
                                   is_synced_to_cloud BOOLEAN DEFAULT FALSE, -- Cờ phục vụ đồng bộ khi trạm có Internet trở lại
                                   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 8. Bảng dispatch_missions (Ca điều phối / Lệnh điều xe thực tế)
CREATE TABLE dispatch_missions (
                                   id SERIAL PRIMARY KEY,
                                   request_id INT REFERENCES dispatch_requests(id) UNIQUE NOT NULL, -- Đảm bảo 1 xe - 1 ca tại một thời điểm
                                   resource_id INT REFERENCES dispatch_resources(id) NOT NULL,
                                   destination_id INT REFERENCES medical_hospitals(id) ON DELETE SET NULL, -- Bệnh viện đích đến
                                   destination_name VARCHAR(255),
                                   status VARCHAR(20) DEFAULT 'DISPATCHED', -- DISPATCHED, ACCEPTED, ON_SCENE, EN_ROUTE, COMPLETED

    -- Dòng thời gian phục vụ phân tích hiệu năng KPI cứu hộ
                                   dispatched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                   accepted_at TIMESTAMP,
                                   on_scene_at TIMESTAMP,