-- =============================================================================
-- V15 — Organization Management authorization contract (global permission seeds)
-- Schema: iam (V1)
-- PostgreSQL 16+
-- FASE 16.3 — Organization Authorization Contract (ADR-010, ADR-007)
-- =============================================================================
-- Extends V13 IAM catalog with Organization / Office / StaffAssignment grants.
-- Idempotent via NOT EXISTS on permission.code.
-- Backfills iam.role_permission for existing system roles (idempotent NOT EXISTS).
-- No clinical/billing permissions — reserved for FASE 17+ / 19+ / 20+.
-- =============================================================================

INSERT INTO iam.permission (
    permission_id,
    code,
    description,
    system_permission,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    v.code,
    v.description,
    TRUE,
    NOW(),
    NOW()
FROM (
    VALUES
        ('organization:create',           'Create organizations'),
        ('organization:read',             'Read organizations'),
        ('organization:update',           'Update organizations'),
        ('organization:archive',          'Archive organizations'),
        ('office:create',                 'Create offices'),
        ('office:read',                   'Read offices'),
        ('office:update',                 'Update offices'),
        ('office:archive',                'Archive offices'),
        ('staff-assignment:create',       'Create staff organizational assignments'),
        ('staff-assignment:read',         'Read staff organizational assignments'),
        ('staff-assignment:update',       'Update staff organizational assignments'),
        ('staff-assignment:delete',       'Delete staff organizational assignments')
) AS v(code, description)
WHERE NOT EXISTS (
    SELECT 1
    FROM iam.permission p
    WHERE p.code = v.code
);

-- -----------------------------------------------------------------------------
-- Backfill system role grants for tenants created before V15
-- -----------------------------------------------------------------------------

INSERT INTO iam.role_permission (role_id, permission_id, assigned_at)
SELECT r.role_id, p.permission_id, NOW()
FROM iam.role r
         CROSS JOIN iam.permission p
WHERE r.system_role = TRUE
  AND r.code = 'OWNER'
  AND p.code IN (
    'organization:create', 'organization:read', 'organization:update', 'organization:archive',
    'office:create', 'office:read', 'office:update', 'office:archive',
    'staff-assignment:create', 'staff-assignment:read', 'staff-assignment:update', 'staff-assignment:delete'
)
  AND NOT EXISTS (
    SELECT 1
    FROM iam.role_permission rp
    WHERE rp.role_id = r.role_id
      AND rp.permission_id = p.permission_id
);

INSERT INTO iam.role_permission (role_id, permission_id, assigned_at)
SELECT r.role_id, p.permission_id, NOW()
FROM iam.role r
         CROSS JOIN iam.permission p
WHERE r.system_role = TRUE
  AND r.code = 'ADMIN'
  AND p.code IN (
    'organization:create', 'organization:read', 'organization:update', 'organization:archive',
    'office:create', 'office:read', 'office:update', 'office:archive',
    'staff-assignment:create', 'staff-assignment:read', 'staff-assignment:update', 'staff-assignment:delete'
)
  AND NOT EXISTS (
    SELECT 1
    FROM iam.role_permission rp
    WHERE rp.role_id = r.role_id
      AND rp.permission_id = p.permission_id
);

INSERT INTO iam.role_permission (role_id, permission_id, assigned_at)
SELECT r.role_id, p.permission_id, NOW()
FROM iam.role r
         CROSS JOIN iam.permission p
WHERE r.system_role = TRUE
  AND r.code = 'MANAGER'
  AND p.code IN (
    'organization:read',
    'office:create', 'office:read', 'office:update', 'office:archive',
    'staff-assignment:create', 'staff-assignment:read', 'staff-assignment:update', 'staff-assignment:delete'
)
  AND NOT EXISTS (
    SELECT 1
    FROM iam.role_permission rp
    WHERE rp.role_id = r.role_id
      AND rp.permission_id = p.permission_id
);

INSERT INTO iam.role_permission (role_id, permission_id, assigned_at)
SELECT r.role_id, p.permission_id, NOW()
FROM iam.role r
         CROSS JOIN iam.permission p
WHERE r.system_role = TRUE
  AND r.code = 'USER'
  AND p.code IN (
    'organization:read', 'office:read', 'staff-assignment:read'
)
  AND NOT EXISTS (
    SELECT 1
    FROM iam.role_permission rp
    WHERE rp.role_id = r.role_id
      AND rp.permission_id = p.permission_id
);

INSERT INTO iam.role_permission (role_id, permission_id, assigned_at)
SELECT r.role_id, p.permission_id, NOW()
FROM iam.role r
         CROSS JOIN iam.permission p
WHERE r.system_role = TRUE
  AND r.code = 'READ_ONLY'
  AND p.code IN (
    'organization:read', 'office:read', 'staff-assignment:read'
)
  AND NOT EXISTS (
    SELECT 1
    FROM iam.role_permission rp
    WHERE rp.role_id = r.role_id
      AND rp.permission_id = p.permission_id
);

COMMENT ON TABLE iam.permission IS
    'Global permission catalog — V13 IAM foundation + V15 Organization Management contract.';
