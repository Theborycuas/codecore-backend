-- =============================================================================
-- V14 — Organization (tenant-scoped business structure aggregate)
-- Schema: org
-- PostgreSQL 16+
-- FASE 16.2 — Organization Persistence (ADR-010)
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS org;

CREATE TABLE org.organization (
    organization_id UUID                     NOT NULL,
    tenant_id       UUID                     NOT NULL,
    code            VARCHAR(64)              NOT NULL,
    name            VARCHAR(200)             NOT NULL,
    status          VARCHAR(50)              NOT NULL,
    created_at      TIMESTAMPTZ              NOT NULL,
    updated_at      TIMESTAMPTZ              NOT NULL,

    CONSTRAINT pk_organization
        PRIMARY KEY (organization_id),

    CONSTRAINT uq_organization_tenant_code
        UNIQUE (tenant_id, code),

    CONSTRAINT ck_organization_status
        CHECK (status IN (
            'ACTIVE',
            'ARCHIVED'
        )),

    CONSTRAINT ck_organization_code_not_blank
        CHECK (char_length(trim(code)) > 0),

    CONSTRAINT ck_organization_name_not_blank
        CHECK (char_length(trim(name)) > 0)
);

CREATE INDEX idx_org_organization_tenant_id
    ON org.organization (tenant_id);

CREATE INDEX idx_org_organization_status
    ON org.organization (status);

COMMENT ON TABLE org.organization IS
    'Organization aggregate root — tenant-scoped business structural unit (ADR-010).';

COMMENT ON COLUMN org.organization.tenant_id IS
    'Logical reference to iam.tenant (domain TenantId). No FK: Organization Management stays decoupled from IAM schema lifecycle (ADR-010).';

COMMENT ON COLUMN org.organization.code IS
    'Functional business identifier (domain OrganizationCode), unique per tenant — e.g. DENTAL_NORTE.';

COMMENT ON COLUMN org.organization.status IS
    'Lifecycle state aligned with OrganizationStatus enum: ACTIVE | ARCHIVED.';
