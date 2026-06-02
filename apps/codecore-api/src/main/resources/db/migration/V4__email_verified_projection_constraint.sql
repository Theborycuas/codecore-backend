-- =============================================================================
-- V4 — email_verified is a DERIVED PROJECTION of status (not master data)
-- Master: iam.iam_user.status  ↔  domain IdentityStatus
-- Projection: email_verified = (status <> 'PENDING_VERIFICATION')
-- =============================================================================

COMMENT ON COLUMN iam.iam_user.email_verified IS
    'DERIVED PROJECTION of status. Not master data. '
    'true when status is not PENDING_VERIFICATION. '
    'Enforced by ck_iam_user_email_verified_projection. '
    'Application must not use this column to rebuild domain lifecycle.';

UPDATE iam.iam_user
SET email_verified = (status <> 'PENDING_VERIFICATION')
WHERE email_verified IS DISTINCT FROM (status <> 'PENDING_VERIFICATION');

ALTER TABLE iam.iam_user
    ADD CONSTRAINT ck_iam_user_email_verified_projection
        CHECK (email_verified = (status <> 'PENDING_VERIFICATION'));
