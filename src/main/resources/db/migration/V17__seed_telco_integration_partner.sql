-- =====================================================================
-- V17__seed_telco_integration_partner.sql
-- Seed dữ liệu đối tác viễn thông mặc định phục vụ SMS OTP
-- =====================================================================

INSERT INTO public.integration_partners (partner_name, partner_type, api_key, api_secret, webhook_url, config, is_active)
VALUES (
    'Viettel OTP Service',
    'TELCO',
    'vt_key_abcdef123456',
    'vt_secret_987654321fedcba',
    'https://api.viettel.vn/webhook/otp',
    '{"sandbox": true, "sms_brandname": "SEMD_OTP", "api_endpoint": "https://api.viettel.vn/sms/send"}'::jsonb,
    true
);
