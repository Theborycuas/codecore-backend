-- =============================================================================
-- V16 — Office (organization-scoped operational unit)
-- Schema: org
-- PostgreSQL 16+
-- FASE 16.5 — Office Domain & Persistence (ADR-010)
-- =============================================================================

CREATE TABLE org.office (
    office_id       UUID                     NOT NULL,
    tenant_id       UUID                     NOT NULL,
    organization_id UUID                     NOT NULL,
    code            VARCHAR(64)              NOT NULL,
    name            VARCHAR(200)             NOT NULL,
    status          VARCHAR(50)              NOT NULL,
    created_at      TIMESTAMPTZ              NOT NULL,
    updated_at      TIMESTAMPTZ              NOT NULL,

    CONSTRAINT pk_office
        PRIMARY KEY (office_id),

    CONSTRAINT uq_office_organization_code
        UNIQUE (organization_id, code),

    CONSTRAINT ck_office_status
        CHECK (status IN (
            'ACTIVE',
            'ARCHIVED'
        )),

    CONSTRAINT ck_office_code_not_blank
        CHECK (char_length(trim(code)) > 0),

    CONSTRAINT ck_office_name_not_blank
        CHECK (char_length(trim(name)) > 0)
);

CREATE INDEX idx_org_office_tenant_id
    ON org.office (tenant_id);

CREATE INDEX idx_org_office_organization_id
    ON org.office (organization_id);

CREATE INDEX idx_org_office_status
    ON org.office (status);

COMMENT ON TABLE org.office IS
    'Office aggregate root — operational unit under an organization (ADR-010).';

COMMENT ON COLUMN org.office.tenant_id IS
    'Denormalized tenant reference for tenant-scoped queries (ADR-003).';

COMMENT ON COLUMN org.office.organization_id IS
    'Logical reference to org.organization (domain OrganizationId). No FK by design.';
