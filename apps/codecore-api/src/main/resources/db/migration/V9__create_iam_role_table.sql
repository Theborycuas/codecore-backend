-- =============================================================================
-- V9 — IAM Role (tenant-scoped authorization aggregate)
-- Schema: iam (V1)
-- PostgreSQL 16+
-- FASE 14.1 — Authorization Foundation
-- =============================================================================

CREATE TABLE iam.role (
    role_id      UUID                     NOT NULL,
    tenant_id    UUID                     NOT NULL,
    code         VARCHAR(100)             NOT NULL,
    name         VARCHAR(200)             NOT NULL,
    status       VARCHAR(50)              NOT NULL,
    system_role  BOOLEAN                  NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ              NOT NULL,
    updated_at   TIMESTAMPTZ              NOT NULL,

    CONSTRAINT pk_role
        PRIMARY KEY (role_id),

    CONSTRAINT uq_role_tenant_code
        UNIQUE (tenant_id, code),

    CONSTRAINT ck_role_status
        CHECK (status IN (
            'ACTIVE',
            'INACTIVE'
        )),

    CONSTRAINT ck_role_code_not_blank
        CHECK (char_length(trim(code)) > 0),

    CONSTRAINT ck_role_name_not_blank
        CHECK (char_length(trim(name)) > 0)
);

COMMENT ON TABLE iam.role IS
    'Tenant-scoped role aggregate — permissions assigned via role_permission (14.3).';

COMMENT ON COLUMN iam.role.tenant_id IS
    'Owning tenant (domain TenantId). Role codes are unique within this tenant.';

COMMENT ON COLUMN iam.role.code IS
    'Stable machine identifier (domain RoleCode), e.g. ADMIN, VET.';

COMMENT ON COLUMN iam.role.system_role IS
    'When true, role is platform-managed and must not be modified by tenant admins.';
