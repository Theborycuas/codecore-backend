-- =============================================================================
-- V20 — Appointment (tenant-scoped planned care commitment)
-- Schema: scheduling
-- PostgreSQL 16+
-- FASE 18.4 — Appointment Persistence (ADR-014 · ADR-003 · ADR-013)
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS scheduling;

CREATE TABLE scheduling.appointment (
    appointment_id        UUID         NOT NULL,
    tenant_id             UUID         NOT NULL,
    patient_id            UUID         NOT NULL,
    staff_assignment_id   UUID         NOT NULL,
    organization_id       UUID         NOT NULL,
    office_id             UUID,
    starts_at             TIMESTAMPTZ  NOT NULL,
    ends_at               TIMESTAMPTZ  NOT NULL,
    status                VARCHAR(50)  NOT NULL,
    created_at            TIMESTAMPTZ  NOT NULL,
    updated_at            TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_appointment
        PRIMARY KEY (appointment_id),

    CONSTRAINT ck_appointment_status
        CHECK (status IN (
            'SCHEDULED',
            'CANCELLED',
            'COMPLETED'
        )),

    CONSTRAINT ck_appointment_ends_after_starts
        CHECK (ends_at > starts_at)
);

CREATE INDEX idx_scheduling_appointment_tenant_id
    ON scheduling.appointment (tenant_id);

CREATE INDEX idx_scheduling_appointment_status
    ON scheduling.appointment (status);

CREATE INDEX idx_scheduling_appointment_tenant_status
    ON scheduling.appointment (tenant_id, status);

CREATE INDEX idx_scheduling_appointment_tenant_patient_id
    ON scheduling.appointment (tenant_id, patient_id);

CREATE INDEX idx_scheduling_appointment_tenant_organization_id
    ON scheduling.appointment (tenant_id, organization_id);

CREATE INDEX idx_scheduling_appointment_tenant_staff_assignment_id
    ON scheduling.appointment (tenant_id, staff_assignment_id);

COMMENT ON SCHEMA scheduling IS
    'Scheduling bounded context schema (Appointment and future scheduling tables).';

COMMENT ON TABLE scheduling.appointment IS
    'Appointment aggregate root — planned commitment to provide a service to a care subject at a determined time and operational context (ADR-014). Intentionally small.';

COMMENT ON COLUMN scheduling.appointment.tenant_id IS
    'Logical reference to iam.tenant (domain TenantId). No FK: Scheduling stays decoupled from IAM schema lifecycle (ADR-003 · ADR-014).';

COMMENT ON COLUMN scheduling.appointment.patient_id IS
    'Logical reference to clinical.patient. No FK by design (ADR-013 · ADR-014). ACTIVE validation via PatientReferencePort on write.';

COMMENT ON COLUMN scheduling.appointment.staff_assignment_id IS
    'Logical reference to org.staff_assignment (who operates). No FK by design (ADR-011 · ADR-013 · ADR-014). Scope coherence via StaffAssignmentReferencePort.';

COMMENT ON COLUMN scheduling.appointment.organization_id IS
    'Logical reference to org.organization — intentionally denormalized commitment context (ADR-014 §7). No FK (ADR-013).';

COMMENT ON COLUMN scheduling.appointment.office_id IS
    'Optional logical reference to org.office. No FK by design (ADR-013 · ADR-014).';

COMMENT ON COLUMN scheduling.appointment.status IS
    'Lifecycle state aligned with AppointmentStatus enum: SCHEDULED | CANCELLED | COMPLETED.';
