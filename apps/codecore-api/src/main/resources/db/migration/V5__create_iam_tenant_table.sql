-- =============================================================================
-- V5 — IAM Tenant (multi-tenant boundary aggregate)
-- Schema: iam (V1)
-- PostgreSQL 16+
-- =============================================================================

CREATE TABLE iam.tenant (
    tenant_id   UUID                     NOT NULL,
    name        VARCHAR(200)             NOT NULL,
    status      VARCHAR(50)              NOT NULL,
    created_at  TIMESTAMPTZ              NOT NULL,
    updated_at  TIMESTAMPTZ              NOT NULL,

    CONSTRAINT pk_tenant
        PRIMARY KEY (tenant_id),

    CONSTRAINT ck_tenant_status
        CHECK (status IN (
            'ACTIVE',
            'SUSPENDED',
            'DISABLED'
        )),

    CONSTRAINT ck_tenant_name_not_blank
        CHECK (char_length(trim(name)) > 0)
);

COMMENT ON TABLE iam.tenant IS
    'Tenant aggregate root — organizational boundary for IAM multi-tenancy.';

COMMENT ON COLUMN iam.tenant.tenant_id IS
    'Primary key (domain TenantId).';

COMMENT ON COLUMN iam.tenant.status IS
    'Lifecycle state aligned with TenantStatus enum in domain layer.';
