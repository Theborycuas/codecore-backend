-- =============================================================================
-- V32 — Password reset request (ADR-009 P1)
-- Schema: iam
-- PostgreSQL 16+
-- =============================================================================

CREATE TABLE iam.password_reset_request (
    id              UUID                     NOT NULL,
    tenant_id       UUID                     NOT NULL,
    identity_id     UUID                     NOT NULL,
    token_hash      VARCHAR(128)             NOT NULL,
    expires_at      TIMESTAMPTZ              NOT NULL,
    status          VARCHAR(50)              NOT NULL,
    used_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ              NOT NULL,
    updated_at      TIMESTAMPTZ              NOT NULL,
    version         BIGINT                   NOT NULL DEFAULT 0,

    CONSTRAINT pk_password_reset_request
        PRIMARY KEY (id),

    CONSTRAINT uq_password_reset_request_token_hash
        UNIQUE (token_hash),

    CONSTRAINT ck_password_reset_request_status
        CHECK (status IN (
            'PENDING',
            'USED',
            'EXPIRED',
            'CANCELLED'
        )),

    CONSTRAINT ck_password_reset_request_token_hash_not_blank
        CHECK (char_length(trim(token_hash)) > 0),

    CONSTRAINT ck_password_reset_request_version_non_negative
        CHECK (version >= 0)
);

COMMENT ON TABLE iam.password_reset_request IS
    'Password recovery aggregate — stores SHA-256 token hash only (never raw token).';

COMMENT ON COLUMN iam.password_reset_request.token_hash IS
    'SHA-256 hex digest of the opaque reset token; raw token is emailed and never persisted.';

COMMENT ON COLUMN iam.password_reset_request.status IS
    'Lifecycle: PENDING | USED | EXPIRED | CANCELLED.';

CREATE INDEX idx_password_reset_request_identity_id
    ON iam.password_reset_request (identity_id);

CREATE INDEX idx_password_reset_request_tenant_id
    ON iam.password_reset_request (tenant_id);

CREATE INDEX idx_password_reset_request_status
    ON iam.password_reset_request (status);
