-- =============================================================================
-- V17 — StaffAssignment (membership ↔ org/office operational scope)
-- Schema: org
-- FASE 16.7 — Staff Organizational Assignment (ADR-010)
-- =============================================================================

CREATE TABLE org.staff_assignment (
    assignment_id   UUID                     NOT NULL,
    tenant_id       UUID                     NOT NULL,
    membership_id   UUID                     NOT NULL,
    organization_id UUID                     NOT NULL,
    office_id       UUID,
    created_at      TIMESTAMPTZ              NOT NULL,
    updated_at      TIMESTAMPTZ              NOT NULL,

    CONSTRAINT pk_staff_assignment
        PRIMARY KEY (assignment_id)
);

CREATE UNIQUE INDEX uq_staff_assignment_org_scope
    ON org.staff_assignment (tenant_id, membership_id, organization_id)
    WHERE office_id IS NULL;

CREATE UNIQUE INDEX uq_staff_assignment_office_scope
    ON org.staff_assignment (tenant_id, membership_id, office_id)
    WHERE office_id IS NOT NULL;

CREATE INDEX idx_staff_assignment_tenant_id
    ON org.staff_assignment (tenant_id);

CREATE INDEX idx_staff_assignment_membership_id
    ON org.staff_assignment (membership_id);

CREATE INDEX idx_staff_assignment_organization_id
    ON org.staff_assignment (organization_id);

CREATE INDEX idx_staff_assignment_office_id
    ON org.staff_assignment (office_id);

COMMENT ON TABLE org.staff_assignment IS
    'StaffAssignment aggregate — links IAM membership to org/office scope (ADR-010).';

COMMENT ON COLUMN org.staff_assignment.membership_id IS
    'Logical reference to iam.identity_tenant_membership (domain MembershipId). No FK by design.';

COMMENT ON COLUMN org.staff_assignment.organization_id IS
    'Required organization scope; office_id NULL means organization-level assignment.';
