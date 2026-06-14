-- =====================================================================
-- V6__integration_partners.sql
-- Quản lý kết nối với bên thứ 3: nhà mạng (SMS/OTP), payment gateway,
-- API bệnh viện / nhà cung cấp dịch vụ
-- =====================================================================

CREATE TABLE public.integration_partners
(
    id             integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    partner_name   varchar(100) NOT NULL,
    partner_type   varchar(20)  NOT NULL
        CHECK (partner_type IN ('TELCO','PAYMENT','HOSPITAL_API','PROVIDER_API')),
    api_key        varchar(255),
    api_secret     varchar(255),
    webhook_url    varchar(512),
    config         jsonb,           -- cấu hình bổ sung: endpoint, sandbox flag, mapping field...
    is_active      boolean DEFAULT true,
    created_at     timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_integration_partners_type ON public.integration_partners(partner_type);

-- Log các lần gọi/nhận webhook từ đối tác - phục vụ debug & audit tích hợp
CREATE TABLE public.integration_logs
(
    id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    partner_id   integer REFERENCES public.integration_partners(id) ON DELETE SET NULL,
    direction    varchar(10) NOT NULL CHECK (direction IN ('INBOUND','OUTBOUND')),
    event_type   varchar(50),
    payload      jsonb,
    status_code  integer,
    created_at   timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_integration_logs_partner ON public.integration_logs(partner_id, created_at DESC);
