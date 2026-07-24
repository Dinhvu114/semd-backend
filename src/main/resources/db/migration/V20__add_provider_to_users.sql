ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS provider_id integer;

ALTER TABLE public.users
    ADD CONSTRAINT fk_users_provider
    FOREIGN KEY (provider_id) REFERENCES public.providers(id) ON DELETE RESTRICT;

CREATE INDEX IF NOT EXISTS idx_users_provider_id ON public.users(provider_id);
