-- =============================================================================
-- V30 — Invitation (access provisioning aggregate)
-- Schema: access
-- PostgreSQL 16+
-- FASE 23.4 — Invitation Persistence (ADR-019 · ADR-003 · ADR-013)
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS access;

CREATE TABLE access.invitation (
    invitation_id                 UUID          NOT NULL,
    tenant_id                     UUID          NOT NULL,
    invited_email                 VARCHAR(320)  NOT NULL,
    invited_role_code             VARCHAR(64)   NOT NULL,
    invited_by_membership_id      UUID          NOT NULL,
    token_hash                    VARCHAR(128)  NOT NULL,
    expires_at                    TIMESTAMPTZ   NOT NULL,
    status                        VARCHAR(20)   NOT NULL,
    resulting_membership_id       UUID,
    created_at                    TIMESTAMPTZ   NOT NULL,
    updated_at                    TIMESTAMPTZ   NOT NULL,
    accepted_at                   TIMESTAMPTZ,
    revoked_at                    TIMESTAMPTZ,

    CONSTRAINT pk_invitation
        PRIMARY KEY (invitation_id),

    CONSTRAINT ck_invitation_status
        CHECK (status IN (
            'PENDING',
            'ACCEPTED',
            'REVOKED',
            'EXPIRED'
        )),

    CONSTRAINT ck_invitation_email_not_blank
        CHECK (char_length(trim(invited_email)) > 0),

    CONSTRAINT ck_invitation_role_not_blank
        CHECK (char_length(trim(invited_role_code)) > 0),

    CONSTRAINT ck_invitation_token_hash_not_blank
        CHECK (char_length(trim(token_hash)) > 0)
);

-- No FK to iam.* by design (ADR-013 / ADR-019): Access stays decoupled from IAM schema
-- lifecycle. Existence + ACTIVE status / system roles are validated at write time via
-- IAM contract ports, not enforced at the database level.

CREATE UNIQUE INDEX uq_access_invitation_token_hash
    ON access.invitation (token_hash);

CREATE INDEX idx_access_invitation_tenant_id
    ON access.invitation (tenant_id);

CREATE INDEX idx_access_invitation_tenant_status
    ON access.invitation (tenant_id, status);

CREATE INDEX idx_access_invitation_tenant_email
    ON access.invitation (tenant_id, invited_email);

COMMENT ON SCHEMA access IS
    'Access bounded context schema (Invitation join-intent aggregate — ADR-019).';

COMMENT ON TABLE access.invitation IS
    'Invitation aggregate root — the pending (and resolved) intent that an identity obtain Membership in a Tenant (ADR-019). Intentionally small: no StaffAssignment, Subscription, PasswordReset, or email transport.';

COMMENT ON COLUMN access.invitation.tenant_id IS
    'Logical reference to iam.tenant (domain TenantId). No FK: Access stays decoupled from IAM schema lifecycle (ADR-003 · ADR-019). Immutable after create.';

COMMENT ON COLUMN access.invitation.invited_by_membership_id IS
    'Logical reference to iam.membership of the inviter. No FK. Validated ACTIVE at create via IamMembershipReferencePort.';

COMMENT ON COLUMN access.invitation.token_hash IS
    'SHA-256 hex digest of the opaque invitation token. Raw token is never stored.';

COMMENT ON COLUMN access.invitation.resulting_membership_id IS
    'Logical reference to iam.membership created/linked on accept. Set-once. No FK.';

COMMENT ON COLUMN access.invitation.status IS
    'Lifecycle state aligned with InvitationStatus enum: PENDING | ACCEPTED | REVOKED | EXPIRED. No DRAFT; no un-revoke; no re-accept; no physical delete.';
