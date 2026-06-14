-- ==================================================================================
-- HỆ THỐNG ĐIỀU PHỐI Y TẾ NGOẠI VIỆN
-- Phiên bản: 1.0
-- ==================================================================================

-- 0. KÍCH HOẠT TIỆN ÍCH MỞ RỘNG KHÔNG GIAN
CREATE EXTENSION IF NOT EXISTS postgis;

-- ==================================================================================
-- NHÓM 1: CÁC BẢNG DANH MỤC VÀ QUẢN LÝ TÀI KHOẢN (Độc lập, không chứa khóa ngoại)
-- ==================================================================================

-- 2. Function dùng cho trigger auto-update updated_at
CREATE OR REPLACE FUNCTION public.update_modified_column()
RETURNS trigger AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 3. Table: users
-- Role mở rộng: ADMIN, DISPATCHER, DRIVER, USER, PROVIDER, HOSPITAL_EMP
CREATE TABLE public.users
(
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username        varchar(50)  NOT NULL UNIQUE,
    password_hash   varchar(255) NOT NULL,
    full_name       varchar(100) NOT NULL,
    phone_number    varchar(15)  NOT NULL UNIQUE,
    email           varchar(100) UNIQUE,
    role            varchar(20)  NOT NULL,
    is_active       boolean DEFAULT true,
    created_at      timestamp DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_role CHECK (role IN ('ADMIN','DISPATCHER','DRIVER','USER','PROVIDER','HOSPITAL_EMP'))
);

CREATE INDEX idx_users_role ON public.users(role);

-- 4. Table: providers (Nhà xe / Phòng khám)
CREATE TABLE public.providers
(
    id                bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    owner_user_id     integer NOT NULL REFERENCES public.users(id) ON DELETE RESTRICT,
    provider_name     varchar(150) NOT NULL,
    provider_type     varchar(20)  NOT NULL CHECK (provider_type IN ('TRANSPORT','CLINIC')),
    business_license  varchar(100),
    contact_phone     varchar(15),
    contact_address   varchar(255),
    commission_rate   numeric(5,2) DEFAULT 0,   -- % hoa hồng platform thu
    is_verified       boolean DEFAULT false,
    is_active         boolean DEFAULT true,
    created_at        timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_providers_owner ON public.providers(owner_user_id);
CREATE INDEX idx_providers_type ON public.providers(provider_type);

-- 5. Table: user_organizations
-- Liên kết user với 1 tổ chức (Provider / Hospital) + vai trò trong tổ chức đó
-- Hữu ích cho: driver thuộc provider, staff bệnh viện, bác sĩ nhiều phòng khám
CREATE TABLE public.user_organizations
(
    id                integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           integer NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    organization_type varchar(20) NOT NULL CHECK (organization_type IN ('PROVIDER','HOSPITAL')),
    organization_id   integer NOT NULL,   -- trỏ tới providers.id hoặc medical_hospitals.id tuỳ organization_type
    org_role          varchar(30) DEFAULT 'STAFF',
    joined_at         timestamp DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, organization_type, organization_id)
);

CREATE INDEX idx_user_orgs_user ON public.user_organizations(user_id);
CREATE INDEX idx_user_orgs_org ON public.user_organizations(organization_type, organization_id);
