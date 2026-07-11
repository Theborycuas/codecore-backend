-- =============================================================================
-- V18 — Patient (tenant-scoped clinical registry identity)
-- Schema: clinical
-- PostgreSQL 16+
-- FASE 17.4 — Patient Persistence (ADR-012 · ADR-003 · ADR-013)
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS clinical;

CREATE TABLE clinical.patient (
    patient_id               UUID         NOT NULL,
    tenant_id                UUID         NOT NULL,
    primary_organization_id  UUID,
    display_name             VARCHAR(200) NOT NULL,
    contact_email            VARCHAR(320),
    contact_phone            VARCHAR(32),
    date_of_birth            DATE,
    status                   VARCHAR(50)  NOT NULL,
    created_at               TIMESTAMPTZ  NOT NULL,
    updated_at               TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_patient
        PRIMARY KEY (patient_id),

    CONSTRAINT ck_patient_status
        CHECK (status IN (
            'ACTIVE',
            'ARCHIVED'
        )),

    CONSTRAINT ck_patient_display_name_not_blank
        CHECK (char_length(trim(display_name)) > 0)
);

CREATE TABLE clinical.patient_external_identifier (
    patient_id        UUID         NOT NULL,
    tenant_id         UUID         NOT NULL,
    identifier_type   VARCHAR(64)  NOT NULL,
    identifier_value  VARCHAR(128) NOT NULL,

    CONSTRAINT pk_patient_external_identifier
        PRIMARY KEY (patient_id, identifier_type),

    CONSTRAINT fk_patient_external_identifier_patient
        FOREIGN KEY (patient_id)
            REFERENCES clinical.patient (patient_id)
            ON DELETE CASCADE,

    CONSTRAINT ck_patient_ext_id_type_not_blank
        CHECK (char_length(trim(identifier_type)) > 0),

    CONSTRAINT ck_patient_ext_id_value_not_blank
        CHECK (char_length(trim(identifier_value)) > 0),

    CONSTRAINT uq_patient_ext_id_tenant_type_value
        UNIQUE (tenant_id, identifier_type, identifier_value)
);

CREATE INDEX idx_clinical_patient_tenant_id
    ON clinical.patient (tenant_id);

CREATE INDEX idx_clinical_patient_status
    ON clinical.patient (status);

CREATE INDEX idx_clinical_patient_tenant_status
    ON clinical.patient (tenant_id, status);

CREATE INDEX idx_clinical_patient_primary_organization_id
    ON clinical.patient (tenant_id, primary_organization_id);

CREATE INDEX idx_clinical_patient_ext_id_patient_id
    ON clinical.patient_external_identifier (patient_id);

CREATE INDEX idx_clinical_patient_ext_id_tenant_id
    ON clinical.patient_external_identifier (tenant_id);

COMMENT ON SCHEMA clinical IS
    'Clinical Foundation bounded context schema (Patient and future clinical registry tables).';

COMMENT ON TABLE clinical.patient IS
    'Patient aggregate root — clinical registry identity of a care subject (ADR-012). Intentionally small.';

COMMENT ON COLUMN clinical.patient.tenant_id IS
    'Logical reference to iam.tenant (domain TenantId). No FK: Clinical Foundation stays decoupled from IAM schema lifecycle (ADR-003 · ADR-012).';

COMMENT ON COLUMN clinical.patient.primary_organization_id IS
    'Optional PrimaryOrganizationId — logical reference to org.organization. No FK by design (ADR-011 · ADR-013). Not ownership.';

COMMENT ON COLUMN clinical.patient.status IS
    'Lifecycle state aligned with PatientStatus enum: ACTIVE | ARCHIVED.';

COMMENT ON TABLE clinical.patient_external_identifier IS
    'Typed external identity keys owned by Patient (MRN, document, chip, …). Soft-unique per tenant (ADR-012).';
