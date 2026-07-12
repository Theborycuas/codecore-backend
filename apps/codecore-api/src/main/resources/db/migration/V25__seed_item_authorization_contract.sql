-- =============================================================================
-- V25 — Item authorization contract (global permission seeds)
-- Schema: iam (V1)
-- PostgreSQL 16+
-- FASE 20.5 — Item Authorization Contract (ADR-016, ADR-007)
-- =============================================================================
-- Extends V13+V15+V19+V21+V23 catalog with Inventory (Item) lifecycle grants.
-- Idempotent via NOT EXISTS on permission.code.
-- Backfills iam.role_permission for existing system roles (idempotent NOT EXISTS).
-- No stock / price / BOM / lot / clinical / vertical-specific permissions.
-- activate maps to item:update (no item:activate permission).
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
        ('item:create',  'Create inventoriable catalog identities (items)'),
        ('item:read',    'Read inventoriable catalog identities (items)'),
        ('item:update',  'Update inventoriable catalog identities (rename, code, org, activate)'),
        ('item:archive', 'Archive inventoriable catalog identities (items)')
) AS v(code, description)
WHERE NOT EXISTS (
    SELECT 1
    FROM iam.permission p
    WHERE p.code = v.code
);

-- -----------------------------------------------------------------------------
-- Backfill system role grants for tenants created before V25
-- Matrix: OWNER/ADMIN/MANAGER = all 4; USER/READ_ONLY = item:read
-- -----------------------------------------------------------------------------

INSERT INTO iam.role_permission (role_id, permission_id, assigned_at)
SELECT r.role_id, p.permission_id, NOW()
FROM iam.role r
         CROSS JOIN iam.permission p
WHERE r.system_role = TRUE
  AND r.code = 'OWNER'
  AND p.code IN (
    'item:create', 'item:read', 'item:update', 'item:archive'
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
    'item:create', 'item:read', 'item:update', 'item:archive'
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
    'item:create', 'item:read', 'item:update', 'item:archive'
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
    'item:read'
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
    'item:read'
)
  AND NOT EXISTS (
    SELECT 1
    FROM iam.role_permission rp
    WHERE rp.role_id = r.role_id
      AND rp.permission_id = p.permission_id
);

COMMENT ON TABLE iam.permission IS
    'Global permission catalog — V13 IAM + V15 Organization + V19 Patient + V21 Appointment + V23 Encounter + V25 Item Inventory.';
