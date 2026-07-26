-- =====================================================================
-- V15__add_location_to_emergency_calls.sql
-- Thêm trường định vị (location) vào bảng emergency_calls
-- =====================================================================

ALTER TABLE public.emergency_calls ADD COLUMN location geography(Point, 4326);
