-- =============================================================================
-- V12 — Membership ↔ Role (N:M association)
-- Schema: iam (V1)
-- PostgreSQL 16+
-- FASE 14.4 — Authorization Foundation
-- =============================================================================

CREATE TABLE iam.membership_role (
    membership_id  UUID                     NOT NULL,
    role_id        UUID                     NOT NULL,
    assigned_at    TIMESTAMPTZ              NOT NULL,

    CONSTRAINT pk_membership_role
        PRIMARY KEY (membership_id, role_id),

    CONSTRAINT uq_membership_role_membership_role
        UNIQUE (membership_id, role_id),

    CONSTRAINT fk_membership_role_membership
        FOREIGN KEY (membership_id)
        REFERENCES iam.identity_tenant_membership (membership_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_membership_role_role
        FOREIGN KEY (role_id)
        REFERENCES iam.role (role_id)
        ON DELETE RESTRICT
);

COMMENT ON TABLE iam.membership_role IS
    'N:M link between IdentityTenantMembership and tenant-scoped Role (owned by Membership aggregate).';

COMMENT ON COLUMN iam.membership_role.membership_id IS
    'Membership aggregate id — tenant scope inherited from identity_tenant_membership (no redundant tenant_id).';

COMMENT ON COLUMN iam.membership_role.role_id IS
    'Role id — must belong to the same tenant as the membership (enforced in domain layer).';
