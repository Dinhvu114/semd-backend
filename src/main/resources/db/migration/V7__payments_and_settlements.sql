-- =====================================================================
-- V7__payments_and_settlements.sql
-- Thanh toán theo mission + đối soát công nợ cho Provider
-- =====================================================================

CREATE TABLE public.payment_transactions
(
    id                     bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    mission_id             integer REFERENCES public.dispatch_missions(id) ON DELETE SET NULL,
    payer_user_id          integer REFERENCES public.users(id) ON DELETE SET NULL,
    provider_id            integer REFERENCES public.providers(id) ON DELETE SET NULL,
    partner_id             integer REFERENCES public.integration_partners(id) ON DELETE SET NULL,
    amount                 numeric(12,2) NOT NULL,
    commission_amount      numeric(12,2) DEFAULT 0,
    payment_method         varchar(30),       -- VNPAY, MOMO, CASH, ...
    external_transaction_id varchar(255),
    status                 varchar(20) DEFAULT 'PENDING'
        CHECK (status IN ('PENDING','SUCCESS','FAILED','REFUNDED')),
    created_at             timestamp DEFAULT CURRENT_TIMESTAMP,
    paid_at                timestamp
);

CREATE INDEX idx_payments_mission ON public.payment_transactions(mission_id);
CREATE INDEX idx_payments_provider ON public.payment_transactions(provider_id);
CREATE INDEX idx_payments_status ON public.payment_transactions(status);

-- Đối soát công nợ định kỳ cho Provider (theo kỳ)
CREATE TABLE public.provider_settlements
(
    id              integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    provider_id     integer NOT NULL REFERENCES public.providers(id) ON DELETE CASCADE,
    period_start    date NOT NULL,
    period_end      date NOT NULL,
    total_revenue   numeric(14,2) DEFAULT 0,
    total_commission numeric(14,2) DEFAULT 0,
    status          varchar(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING','PAID')),
    created_at      timestamp DEFAULT CURRENT_TIMESTAMP,
    paid_at         timestamp
);

CREATE INDEX idx_settlements_provider ON public.provider_settlements(provider_id, period_start);
