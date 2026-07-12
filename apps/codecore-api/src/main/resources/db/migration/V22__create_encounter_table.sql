-- =============================================================================
-- V22 — Encounter (tenant-scoped occurred care episode)
-- Schema: records
-- PostgreSQL 16+
-- FASE 19.4 — Encounter Persistence (ADR-015 · ADR-003 · ADR-013)
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS records;

CREATE TABLE records.encounter (
    encounter_id          UUID         NOT NULL,
    tenant_id             UUID         NOT NULL,
    patient_id            UUID         NOT NULL,
    staff_assignment_id   UUID         NOT NULL,
    organization_id       UUID         NOT NULL,
    office_id             UUID,
    appointment_id        UUID,
    started_at            TIMESTAMPTZ  NOT NULL,
    ended_at              TIMESTAMPTZ,
    status                VARCHAR(50)  NOT NULL,
    created_at            TIMESTAMPTZ  NOT NULL,
    updated_at            TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_encounter
        PRIMARY KEY (encounter_id),

    CONSTRAINT ck_encounter_status
        CHECK (status IN (
            'IN_PROGRESS',
            'CANCELLED',
            'COMPLETED'
        )),

    CONSTRAINT ck_encounter_ended_at_not_before_started
        CHECK (ended_at IS NULL OR ended_at >= started_at)
);

CREATE INDEX idx_records_encounter_tenant_id
    ON records.encounter (tenant_id);

CREATE INDEX idx_records_encounter_status
    ON records.encounter (status);

CREATE INDEX idx_records_encounter_tenant_status
    ON records.encounter (tenant_id, status);

CREATE INDEX idx_records_encounter_tenant_patient_id
    ON records.encounter (tenant_id, patient_id);

CREATE INDEX idx_records_encounter_tenant_organization_id
    ON records.encounter (tenant_id, organization_id);

CREATE INDEX idx_records_encounter_tenant_staff_assignment_id
    ON records.encounter (tenant_id, staff_assignment_id);

CREATE INDEX idx_records_encounter_tenant_appointment_id
    ON records.encounter (tenant_id, appointment_id)
    WHERE appointment_id IS NOT NULL;

COMMENT ON SCHEMA records IS
    'Clinical Records bounded context schema (Encounter and future documentation tables).';

COMMENT ON TABLE records.encounter IS
    'Encounter aggregate root — care episode that occurred for a care subject at a determined operational context (ADR-015). Intentionally small.';

COMMENT ON COLUMN records.encounter.tenant_id IS
    'Logical reference to iam.tenant (domain TenantId). No FK: Records stays decoupled from IAM schema lifecycle (ADR-003 · ADR-015).';

COMMENT ON COLUMN records.encounter.patient_id IS
    'Logical reference to clinical.patient. No FK by design (ADR-013 · ADR-015). ACTIVE validation via PatientReferencePort on write.';

COMMENT ON COLUMN records.encounter.staff_assignment_id IS
    'Logical reference to org.staff_assignment (who operated). No FK by design (ADR-011 · ADR-013 · ADR-015).';

COMMENT ON COLUMN records.encounter.organization_id IS
    'Logical reference to org.organization — intentionally denormalized episode context (ADR-015 §7). No FK (ADR-013).';

COMMENT ON COLUMN records.encounter.office_id IS
    'Optional logical reference to org.office. No FK by design (ADR-013 · ADR-015).';

COMMENT ON COLUMN records.encounter.appointment_id IS
    'Optional logical reference to scheduling.appointment (planned origin). No FK by design (ADR-013 · ADR-014 · ADR-015). Linkable validation via AppointmentReferencePort.';

COMMENT ON COLUMN records.encounter.status IS
    'Lifecycle state aligned with EncounterStatus enum: IN_PROGRESS | CANCELLED | COMPLETED.';
