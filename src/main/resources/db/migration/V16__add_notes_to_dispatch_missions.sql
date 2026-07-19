-- =====================================================================
-- V16__add_notes_to_dispatch_missions.sql
-- Thêm cột notes vào bảng dispatch_missions
-- =====================================================================

ALTER TABLE public.dispatch_missions ADD COLUMN notes text;
