-- =============================================================================
-- V34 — Audit authorization contract (global permission seeds)
-- Schema: iam (V1)
-- PostgreSQL 16+
-- FASE 24.5 — Audit Authorization Contract (ADR-020, ADR-007)
-- =============================================================================
-- Extends V13+…+V31 catalog with Audit (AuditEntry) read grant.
-- Idempotent via NOT EXISTS on permission.code.
-- Backfills iam.role_permission for existing system roles (idempotent NOT EXISTS).
-- No audit:create / audit:update / audit:delete — append is via AuditAppendPort.
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
        ('audit:read', 'Read AuditEntry records for the current tenant')
) AS v(code, description)
WHERE NOT EXISTS (
    SELECT 1
    FROM iam.permission p
    WHERE p.code = v.code
);

-- -----------------------------------------------------------------------------
-- Backfill system role grants for tenants created before V34
-- Matrix: OWNER/ADMIN/MANAGER/USER/READ_ONLY = audit:read (like Payment read for USER/READ_ONLY)
-- -----------------------------------------------------------------------------

INSERT INTO iam.role_permission (role_id, permission_id, assigned_at)
SELECT r.role_id, p.permission_id, NOW()
FROM iam.role r
         CROSS JOIN iam.permission p
WHERE r.system_role = TRUE
  AND r.code = 'OWNER'
  AND p.code IN (
    'audit:read'
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
    'audit:read'
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
    'audit:read'
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
    'audit:read'
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
    'audit:read'
)
  AND NOT EXISTS (
    SELECT 1
    FROM iam.role_permission rp
    WHERE rp.role_id = r.role_id
      AND rp.permission_id = p.permission_id
);

COMMENT ON TABLE iam.permission IS
    'Global permission catalog — V13 IAM + V15 Organization + V19 Patient + V21 Appointment + V23 Encounter + V25 Item Inventory + V27 Invoice Billing + V29 Payment + V31 Invitation + V34 Audit.';
