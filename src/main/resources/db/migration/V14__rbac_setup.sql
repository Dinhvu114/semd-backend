-- ============================================================
-- V14__rbac_setup.sql
-- Chuyển đổi hệ thống phân quyền sang RBAC (users, roles, user_roles)
-- ============================================================

-- 1. Tạo bảng roles
CREATE TABLE public.roles (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name varchar(50) NOT NULL UNIQUE
);

-- 2. Seed 5 quyền mới theo yêu cầu: ADMIN, DISPATCHER, DRIVER, REPORTER, PROVIDER_ADMIN
INSERT INTO public.roles (name) VALUES
('ADMIN'),
('DISPATCHER'),
('DRIVER'),
('REPORTER'),
('PROVIDER_ADMIN')
ON CONFLICT (name) DO NOTHING;

-- 3. Tạo bảng liên kết user_roles
CREATE TABLE public.user_roles (
    user_id bigint NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    role_id bigint NOT NULL REFERENCES public.roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- 4. Chuyển đổi dữ liệu vai trò hiện tại từ users.role sang user_roles
INSERT INTO public.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM public.users u
JOIN public.roles r ON (
    (u.role = 'ADMIN' AND r.name = 'ADMIN') OR
    (u.role = 'DISPATCHER' AND r.name = 'DISPATCHER') OR
    (u.role = 'DRIVER' AND r.name = 'DRIVER') OR
    (u.role = 'USER' AND r.name = 'REPORTER') OR
    (u.role = 'HOSPITAL_EMP' AND r.name = 'REPORTER') OR
    (u.role = 'PROVIDER' AND r.name = 'PROVIDER_ADMIN')
);

-- 5. Dọn dẹp cấu trúc cũ trong bảng users
DROP INDEX IF EXISTS public.idx_users_role;
ALTER TABLE public.users DROP CONSTRAINT IF EXISTS check_role;
ALTER TABLE public.users DROP COLUMN IF EXISTS role;
