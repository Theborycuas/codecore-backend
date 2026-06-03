-- =============================================================================
-- V7 — Identity ↔ Tenant membership (N:M association)
-- Schema: iam (V1)
-- PostgreSQL 16+
-- =============================================================================

CREATE TABLE iam.identity_tenant_membership (
    membership_id   UUID                     NOT NULL,
    identity_id     UUID                     NOT NULL,
    tenant_id       UUID                     NOT NULL,
    status          VARCHAR(50)              NOT NULL,
    created_at      TIMESTAMPTZ              NOT NULL,
    updated_at      TIMESTAMPTZ              NOT NULL,

    CONSTRAINT pk_identity_tenant_membership
        PRIMARY KEY (membership_id),

    CONSTRAINT uq_identity_tenant_membership_identity_tenant
        UNIQUE (identity_id, tenant_id),

    CONSTRAINT ck_identity_tenant_membership_status
        CHECK (status IN (
            'ACTIVE',
            'INACTIVE'
        ))
);

COMMENT ON TABLE iam.identity_tenant_membership IS
    'N:M link between global Identity and Tenant (formal membership; no roles).';

COMMENT ON COLUMN iam.identity_tenant_membership.membership_id IS
    'Surrogate primary key (domain MembershipId).';

COMMENT ON COLUMN iam.identity_tenant_membership.identity_id IS
    'Identity aggregate id (domain IdentityId).';

COMMENT ON COLUMN iam.identity_tenant_membership.tenant_id IS
    'Tenant aggregate id (domain TenantId).';
