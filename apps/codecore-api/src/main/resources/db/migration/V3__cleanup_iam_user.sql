-- =============================================================================
-- V3 — IAM user cleanup (domain ↔ persistence alignment, PASO 10.5.3)
-- Profile names belong to user-management, not IAM.
-- =============================================================================

ALTER TABLE iam.iam_user
    DROP COLUMN IF EXISTS first_name,
    DROP COLUMN IF EXISTS last_name;
