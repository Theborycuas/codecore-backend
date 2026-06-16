-- =============================================================================
-- V13 — IAM authorization foundation catalog (global system permissions)
-- Schema: iam (V1)
-- PostgreSQL 16+
-- FASE 14.8 — Authorization Foundation Seeds
-- =============================================================================
-- Platform IAM contract only — no business-module permissions (patient:*, etc.).
-- Idempotent via NOT EXISTS on permission.code (safe for deploy 1, 2, 3…).
-- Tenant-scoped system roles (OWNER, ADMIN, …) are provisioned in application
-- code when a tenant is created (roles require tenant_id).
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
        ('tenant:read',        'Read tenant metadata'),
        ('tenant:update',      'Update tenant metadata'),
        ('membership:read',    'Read tenant memberships'),
        ('membership:create',  'Create tenant memberships'),
        ('membership:update',  'Update tenant memberships'),
        ('membership:delete',  'Delete tenant memberships'),
        ('role:read',          'Read tenant roles'),
        ('role:create',        'Create tenant roles'),
        ('role:update',        'Update tenant roles'),
        ('role:delete',        'Delete tenant roles'),
        ('permission:read',    'Read global permission catalog'),
        ('permission:assign',  'Assign permissions to roles'),
        ('user:read',          'Read tenant users'),
        ('user:create',        'Create tenant users'),
        ('user:update',        'Update tenant users'),
        ('user:delete',        'Delete tenant users')
) AS v(code, description)
WHERE NOT EXISTS (
    SELECT 1
    FROM iam.permission p
    WHERE p.code = v.code
);

COMMENT ON TABLE iam.permission IS
    'Global permission catalog — V13 seeds IAM foundation grants (system_permission = true).';
