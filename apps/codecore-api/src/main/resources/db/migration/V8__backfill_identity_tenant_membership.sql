-- =============================================================================
-- V8 — Backfill identity_tenant_membership from iam_user (PASO 12.5)
-- Schema: iam (V1)
-- PostgreSQL 16+
-- =============================================================================
-- Creates ACTIVE membership rows for historical identities that predate PASO 12.3
-- registration flow. Idempotent via NOT EXISTS on (identity_id, tenant_id).
--
-- membership_id: gen_random_uuid() — surrogate key (same role as MembershipId.generate()
-- in application code; prior Flyway scripts V1–V7 do not insert UUIDs in SQL).
-- =============================================================================

INSERT INTO iam.identity_tenant_membership (
    membership_id,
    identity_id,
    tenant_id,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    u.id,
    u.tenant_id,
    'ACTIVE',
    u.created_at,
    u.updated_at
FROM iam.iam_user u
WHERE NOT EXISTS (
    SELECT 1
    FROM iam.identity_tenant_membership m
    WHERE m.identity_id = u.id
      AND m.tenant_id   = u.tenant_id
);

-- =============================================================================
-- Verification queries (manual / post-deploy audit — not executed by Flyway)
-- =============================================================================
--
-- A) Identities without canonical membership (expect 0 rows after V8):
--
-- SELECT u.id, u.tenant_id, u.email, u.status
-- FROM iam.iam_user u
-- LEFT JOIN iam.identity_tenant_membership m
--   ON m.identity_id = u.id AND m.tenant_id = u.tenant_id
-- WHERE m.membership_id IS NULL;
--
-- B) Orphan memberships (identity row missing — expect 0 rows):
--
-- SELECT m.*
-- FROM iam.identity_tenant_membership m
-- LEFT JOIN iam.iam_user u ON u.id = m.identity_id
-- WHERE u.id IS NULL;
--
-- C) Totals:
--
-- SELECT
--   (SELECT COUNT(*) FROM iam.iam_user) AS iam_user_count,
--   (SELECT COUNT(*) FROM iam.identity_tenant_membership) AS membership_count,
--   (SELECT COUNT(*) FROM iam.iam_user u
--    WHERE EXISTS (
--      SELECT 1 FROM iam.identity_tenant_membership m
--      WHERE m.identity_id = u.id AND m.tenant_id = u.tenant_id
--    )) AS users_with_canonical_membership;
