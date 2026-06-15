-- =============================================================================
-- V10 — IAM Permission (global authorization catalog)
-- Schema: iam (V1)
-- PostgreSQL 16+
-- FASE 14.2 — Authorization Foundation
-- =============================================================================

CREATE TABLE iam.permission (
    permission_id     UUID                     NOT NULL,
    code              VARCHAR(150)             NOT NULL,
    description       VARCHAR(500),
    system_permission BOOLEAN                  NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ              NOT NULL,
    updated_at        TIMESTAMPTZ              NOT NULL,

    CONSTRAINT pk_permission
        PRIMARY KEY (permission_id),

    CONSTRAINT uq_permission_code
        UNIQUE (code),

    CONSTRAINT ck_permission_code_not_blank
        CHECK (char_length(trim(code)) > 0)
);

COMMENT ON TABLE iam.permission IS
    'Global permission catalog — atomic resource:action grants (ADR-007).';

COMMENT ON COLUMN iam.permission.code IS
    'Globally unique permission code (domain PermissionCode), e.g. user:create.';

COMMENT ON COLUMN iam.permission.system_permission IS
    'When true, permission is platform-managed and must not be modified.';
