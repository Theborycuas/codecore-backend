-- =============================================================================
-- V33 — AuditEntry (append-only audit fact aggregate)
-- Schema: audit
-- PostgreSQL 16+
-- FASE 24.4 — Audit Persistence (ADR-020 · ADR-003 · ADR-013)
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS audit;

CREATE TABLE audit.audit_entry (
    audit_entry_id          UUID          NOT NULL,
    tenant_id               UUID          NOT NULL,
    occurred_at             TIMESTAMPTZ   NOT NULL,
    action_code             VARCHAR(64)   NOT NULL,
    actor_membership_id     UUID,
    resource_type           VARCHAR(64)   NOT NULL,
    resource_id             UUID          NOT NULL,
    outcome                 VARCHAR(20)   NOT NULL,
    created_at              TIMESTAMPTZ   NOT NULL,

    CONSTRAINT pk_audit_entry
        PRIMARY KEY (audit_entry_id),

    CONSTRAINT ck_audit_entry_outcome
        CHECK (outcome IN (
            'SUCCESS',
            'FAILURE'
        )),

    CONSTRAINT ck_audit_entry_action_code_not_blank
        CHECK (char_length(trim(action_code)) > 0),

    CONSTRAINT ck_audit_entry_resource_type_not_blank
        CHECK (char_length(trim(resource_type)) > 0)
);

-- No FK to iam.membership / iam.tenant by design (ADR-013 / ADR-020): Audit stays decoupled
-- from IAM schema lifecycle. Optional actor ACTIVE status is validated at append time via
-- IamMembershipReferencePort, not enforced at the database level.

CREATE INDEX idx_audit_entry_tenant_id
    ON audit.audit_entry (tenant_id);

CREATE INDEX idx_audit_entry_tenant_occurred_at
    ON audit.audit_entry (tenant_id, occurred_at DESC);

CREATE INDEX idx_audit_entry_tenant_action_code
    ON audit.audit_entry (tenant_id, action_code);

CREATE INDEX idx_audit_entry_tenant_resource
    ON audit.audit_entry (tenant_id, resource_type, resource_id);

COMMENT ON SCHEMA audit IS
    'Audit bounded context schema (AuditEntry append-only aggregate — ADR-020).';

COMMENT ON TABLE audit.audit_entry IS
    'AuditEntry aggregate root — the immutable record that a significant action occurred within a Tenant (ADR-020). Append-only: no update, void, or delete.';

COMMENT ON COLUMN audit.audit_entry.tenant_id IS
    'Logical reference to iam.tenant (domain TenantId). No FK: Audit stays decoupled from IAM schema lifecycle (ADR-003 · ADR-020). Immutable after append.';

COMMENT ON COLUMN audit.audit_entry.actor_membership_id IS
    'Optional logical reference to iam.membership. No FK by design (ADR-013 · ADR-020). When present, ACTIVE status validated at append via IamMembershipReferencePort.';

COMMENT ON COLUMN audit.audit_entry.action_code IS
    'Opaque action code (e.g. invitation.created), capped at 64 chars. Not free text.';

COMMENT ON COLUMN audit.audit_entry.resource_type IS
    'Opaque resource type label (e.g. invitation), capped at 64 chars. Not validated against a catalog.';

COMMENT ON COLUMN audit.audit_entry.resource_id IS
    'Opaque resource UUID. No cross-BC existence check at append time.';

COMMENT ON COLUMN audit.audit_entry.outcome IS
    'Outcome aligned with AuditOutcome enum: SUCCESS | FAILURE. Default SUCCESS at append.';
