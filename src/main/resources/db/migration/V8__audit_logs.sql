-- =====================================================================
-- V8__audit_logs.sql
-- Audit log chung cho toàn hệ thống
-- =====================================================================

CREATE TABLE public.audit_logs
(
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    table_name  varchar(50) NOT NULL,
    record_id   bigint NOT NULL,
    operation   varchar(10) NOT NULL,   -- INSERT, UPDATE, DELETE
    changed_by  integer REFERENCES public.users(id) ON DELETE NO ACTION,
    old_data    jsonb,
    new_data    jsonb,
    changed_at  timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_table_record
    ON public.audit_logs(table_name, record_id);
CREATE INDEX idx_audit_logs_time
    ON public.audit_logs(changed_at DESC);
