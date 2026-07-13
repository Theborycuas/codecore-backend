-- =============================================================================
-- V31 — Invitation authorization contract (global permission seeds)
-- Schema: iam (V1)
-- PostgreSQL 16+
-- FASE 23.5 — Invitation Authorization Contract (ADR-019, ADR-007)
-- =============================================================================
-- Extends V13+…+V29 catalog with Access (Invitation) lifecycle grants.
-- Idempotent via NOT EXISTS on permission.code.
-- Backfills iam.role_permission for existing system roles (idempotent NOT EXISTS).
-- No resend / seat / subscription / vertical-specific permissions.
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
        ('invitation:create', 'Create a Tenant invitation for an email + system role'),
        ('invitation:read',   'Read Tenant invitations'),
        ('invitation:revoke', 'Revoke a PENDING Tenant invitation')
) AS v(code, description)
WHERE NOT EXISTS (
    SELECT 1
    FROM iam.permission p
    WHERE p.code = v.code
);

-- -----------------------------------------------------------------------------
-- Backfill system role grants for tenants created before V31
-- Matrix: OWNER/ADMIN/MANAGER = all 3; USER/READ_ONLY = invitation:read
-- -----------------------------------------------------------------------------

INSERT INTO iam.role_permission (role_id, permission_id, assigned_at)
SELECT r.role_id, p.permission_id, NOW()
FROM iam.role r
         CROSS JOIN iam.permission p
WHERE r.system_role = TRUE
  AND r.code = 'OWNER'
  AND p.code IN (
    'invitation:create', 'invitation:read', 'invitation:revoke'
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
    'invitation:create', 'invitation:read', 'invitation:revoke'
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
    'invitation:create', 'invitation:read', 'invitation:revoke'
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
    'invitation:read'
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
    'invitation:read'
)
  AND NOT EXISTS (
    SELECT 1
    FROM iam.role_permission rp
    WHERE rp.role_id = r.role_id
      AND rp.permission_id = p.permission_id
);

COMMENT ON TABLE iam.permission IS
    'Global permission catalog — V13 IAM + V15 Organization + V19 Patient + V21 Appointment + V23 Encounter + V25 Item Inventory + V27 Invoice Billing + V29 Payment + V31 Invitation Access.';
