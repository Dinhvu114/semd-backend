-- Repair Provider membership and incorrect Provider assignments from V30.

-- 1. Create Provider Admin accounts for the three new Providers.
INSERT INTO public.users
    (username, password_hash, full_name, phone_number, email, is_active)
VALUES
    (
        'provider06',
        '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92',
        'Trung tâm 115 Hà Đông',
        '0246000006',
        'provider06@semd.vn',
        TRUE
    ),
    (
        'provider07',
        '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92',
        'Trung tâm 115 Dương Nội',
        '0246000007',
        'provider07@semd.vn',
        TRUE
    ),
    (
        'provider08',
        '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92',
        'Trung tâm 115 Yên Nghĩa',
        '0246000008',
        'provider08@semd.vn',
        TRUE
    )
ON CONFLICT (username) DO NOTHING;


-- 2. Assign PROVIDER_ADMIN role.
INSERT INTO public.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM public.users u
CROSS JOIN public.roles r
WHERE u.username IN ('provider06', 'provider07', 'provider08')
  AND r.name = 'PROVIDER_ADMIN'
ON CONFLICT (user_id, role_id) DO NOTHING;


-- 3. Create the missing Providers.
INSERT INTO public.providers
    (
        owner_user_id,
        provider_name,
        provider_type,
        business_license,
        contact_phone,
        contact_address,
        commission_rate,
        is_verified,
        is_active
    )
SELECT
    u.id,
    v.provider_name,
    'TRANSPORT',
    v.business_license,
    v.contact_phone,
    v.contact_address,
    10,
    TRUE,
    TRUE
FROM (
    VALUES
        (
            'provider06',
            'Trung tâm Cấp cứu 115 Hà Đông',
            'GP-HD-2024-006',
            '0246000006',
            'Hà Đông, Hà Nội'
        ),
        (
            'provider07',
            'Trung tâm Cấp cứu 115 Dương Nội',
            'GP-DN-2024-007',
            '0246000007',
            'Dương Nội, Hà Nội'
        ),
        (
            'provider08',
            'Trung tâm Cấp cứu 115 Yên Nghĩa',
            'GP-YN-2024-008',
            '0246000008',
            'Yên Nghĩa, Hà Nội'
        )
) AS v(username, provider_name, business_license, contact_phone, contact_address)
JOIN public.users u
    ON u.username = v.username
WHERE NOT EXISTS (
    SELECT 1
    FROM public.providers p
    WHERE p.owner_user_id = u.id
       OR p.provider_name = v.provider_name
);

-- 4. Link all Provider owners to their Provider.
-- This also backfills provider01-provider05.
UPDATE public.users u
SET provider_id = p.id
FROM public.providers p
WHERE p.owner_user_id = u.id
  AND u.provider_id IS NULL;


-- 5. Repair the incorrect V30 Driver assignments.
UPDATE public.users d
SET provider_id = p.id
FROM public.providers p
JOIN public.users owner
    ON owner.id = p.owner_user_id
WHERE owner.username = 'provider06'
  AND d.username = 'driver06';

UPDATE public.users d
SET provider_id = p.id
FROM public.providers p
JOIN public.users owner
    ON owner.id = p.owner_user_id
WHERE owner.username = 'provider07'
  AND d.username = 'driver07';

UPDATE public.users d
SET provider_id = p.id
FROM public.providers p
JOIN public.users owner
    ON owner.id = p.owner_user_id
WHERE owner.username = 'provider08'
  AND d.username = 'driver08';


-- 6. Repair the incorrect V30 ambulance assignments.
UPDATE public.dispatch_resources r
SET provider_id = p.id
FROM public.providers p
JOIN public.users owner
    ON owner.id = p.owner_user_id
WHERE owner.username = 'provider06'
  AND r.resource_code = 'AMB-HD-001';

UPDATE public.dispatch_resources r
SET provider_id = p.id
FROM public.providers p
JOIN public.users owner
    ON owner.id = p.owner_user_id
WHERE owner.username = 'provider07'
  AND r.resource_code = 'AMB-DN-001';

UPDATE public.dispatch_resources r
SET provider_id = p.id
FROM public.providers p
JOIN public.users owner
    ON owner.id = p.owner_user_id
WHERE owner.username = 'provider08'
  AND r.resource_code = 'AMB-YN-001';


-- 7. Backfill legacy drivers from their assigned ambulance.
UPDATE public.users u
SET provider_id = r.provider_id
FROM public.dispatch_resources r
WHERE r.current_driver_id = u.id
  AND u.provider_id IS NULL;


-- 8. Validate Provider owner membership.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM public.providers p
        JOIN public.users u ON u.id = p.owner_user_id
        WHERE u.provider_id IS DISTINCT FROM p.id
    ) THEN
        RAISE EXCEPTION 'Provider owner membership is inconsistent';
    END IF;
END
$$;


-- 9. Validate Driver / ambulance Provider membership.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM public.dispatch_resources r
        JOIN public.users u ON u.id = r.current_driver_id
        WHERE r.current_driver_id IS NOT NULL
          AND u.provider_id IS DISTINCT FROM r.provider_id
    ) THEN
        RAISE EXCEPTION 'Driver and ambulance belong to different Providers';
    END IF;
END
$$;