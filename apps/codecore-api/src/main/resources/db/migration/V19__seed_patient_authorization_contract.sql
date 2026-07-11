-- =============================================================================
-- V19 — Patient authorization contract (global permission seeds)
-- Schema: iam (V1)
-- PostgreSQL 16+
-- FASE 17.5 — Patient Authorization Contract (ADR-012, ADR-007)
-- =============================================================================
-- Extends V13+V15 catalog with Clinical Foundation (Patient) registry grants.
-- Idempotent via NOT EXISTS on permission.code.
-- Backfills iam.role_permission for existing system roles (idempotent NOT EXISTS).
-- No appointment / medical-record / billing / vertical-specific permissions.
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
        ('patient:create',  'Create patient clinical registry identities'),
        ('patient:read',    'Read patient clinical registry identities'),
        ('patient:update',  'Update patient clinical registry identities'),
        ('patient:archive', 'Archive patient clinical registry identities')
) AS v(code, description)
WHERE NOT EXISTS (
    SELECT 1
    FROM iam.permission p
    WHERE p.code = v.code
);

-- -----------------------------------------------------------------------------
-- Backfill system role grants for tenants created before V19
-- Matrix: OWNER/ADMIN/MANAGER = all 4; USER/READ_ONLY = patient:read
-- -----------------------------------------------------------------------------

INSERT INTO iam.role_permission (role_id, permission_id, assigned_at)
SELECT r.role_id, p.permission_id, NOW()
FROM iam.role r
         CROSS JOIN iam.permission p
WHERE r.system_role = TRUE
  AND r.code = 'OWNER'
  AND p.code IN (
    'patient:create', 'patient:read', 'patient:update', 'patient:archive'
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
    'patient:create', 'patient:read', 'patient:update', 'patient:archive'
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
    'patient:create', 'patient:read', 'patient:update', 'patient:archive'
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
    'patient:read'
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
    'patient:read'
)
  AND NOT EXISTS (
    SELECT 1
    FROM iam.role_permission rp
    WHERE rp.role_id = r.role_id
      AND rp.permission_id = p.permission_id
);

COMMENT ON TABLE iam.permission IS
    'Global permission catalog — V13 IAM + V15 Organization + V19 Patient Clinical Foundation.';
