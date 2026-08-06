ALTER TABLE public.emergency_calls
    ADD COLUMN IF NOT EXISTS description TEXT;
