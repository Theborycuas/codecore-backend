-- =============================================================================
-- V26 — Invoice + InvoiceLine (commercial claim aggregate)
-- Schema: billing
-- PostgreSQL 16+
-- FASE 21.4 — Invoice Persistence (ADR-017 · ADR-003 · ADR-013)
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS billing;

CREATE TABLE billing.invoice (
    invoice_id                 UUID          NOT NULL,
    tenant_id                  UUID          NOT NULL,
    issuer_organization_id     UUID          NOT NULL,
    bill_to_patient_id         UUID,
    bill_to_organization_id    UUID,
    invoice_number              VARCHAR(64),
    currency                   VARCHAR(3)    NOT NULL,
    status                     VARCHAR(20)   NOT NULL,
    created_at                 TIMESTAMPTZ   NOT NULL,
    updated_at                 TIMESTAMPTZ   NOT NULL,

    CONSTRAINT pk_invoice
        PRIMARY KEY (invoice_id),

    CONSTRAINT ck_invoice_status
        CHECK (status IN (
            'DRAFT',
            'ISSUED',
            'VOIDED'
        )),

    CONSTRAINT ck_invoice_bill_to_xor
        CHECK ((bill_to_patient_id IS NOT NULL) <> (bill_to_organization_id IS NOT NULL)),

    CONSTRAINT ck_invoice_bill_to_org_not_issuer
        CHECK (bill_to_organization_id IS NULL OR bill_to_organization_id <> issuer_organization_id),

    CONSTRAINT ck_invoice_number_not_blank
        CHECK (invoice_number IS NULL OR char_length(trim(invoice_number)) > 0),

    CONSTRAINT ck_invoice_currency_iso
        CHECK (currency ~ '^[A-Z]{3}$')
);

-- Soft-unique human invoice number per tenant (ADR-017 §5): multiple NULLs allowed.
CREATE UNIQUE INDEX uq_billing_invoice_tenant_number
    ON billing.invoice (tenant_id, invoice_number)
    WHERE invoice_number IS NOT NULL;

CREATE INDEX idx_billing_invoice_tenant_id
    ON billing.invoice (tenant_id);

CREATE INDEX idx_billing_invoice_status
    ON billing.invoice (status);

CREATE INDEX idx_billing_invoice_tenant_status
    ON billing.invoice (tenant_id, status);

CREATE INDEX idx_billing_invoice_issuer_organization_id
    ON billing.invoice (tenant_id, issuer_organization_id);

CREATE TABLE billing.invoice_line (
    line_id            UUID          NOT NULL,
    invoice_id         UUID          NOT NULL,
    tenant_id          UUID          NOT NULL,
    description        VARCHAR(500)  NOT NULL,
    currency           VARCHAR(3)    NOT NULL,
    amount_minor       BIGINT        NOT NULL,
    item_id            UUID,
    encounter_id       UUID,

    CONSTRAINT pk_invoice_line
        PRIMARY KEY (line_id),

    -- Same-aggregate reference (Invoice + InvoiceLine are one aggregate) — not a cross-BC FK.
    CONSTRAINT fk_invoice_line_invoice
        FOREIGN KEY (invoice_id)
        REFERENCES billing.invoice (invoice_id)
        ON DELETE CASCADE,

    CONSTRAINT ck_invoice_line_description_not_blank
        CHECK (char_length(trim(description)) > 0),

    CONSTRAINT ck_invoice_line_amount_positive
        CHECK (amount_minor > 0),

    CONSTRAINT ck_invoice_line_currency_iso
        CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE INDEX idx_billing_invoice_line_invoice_id
    ON billing.invoice_line (invoice_id);

CREATE INDEX idx_billing_invoice_line_tenant_id
    ON billing.invoice_line (tenant_id);

CREATE INDEX idx_billing_invoice_line_item_id
    ON billing.invoice_line (tenant_id, item_id);

CREATE INDEX idx_billing_invoice_line_encounter_id
    ON billing.invoice_line (tenant_id, encounter_id);

COMMENT ON SCHEMA billing IS
    'Billing bounded context schema (Invoice commercial claim aggregate; future Payment tables).';

COMMENT ON TABLE billing.invoice IS
    'Invoice aggregate root — commercial claim of an amount owed by a bill-to party under a Tenant (ADR-017). Intentionally small: no payments, tax breakdown, ledger, stock, or subscriptions.';

COMMENT ON COLUMN billing.invoice.tenant_id IS
    'Logical reference to iam.tenant (domain TenantId). No FK: Billing stays decoupled from IAM schema lifecycle (ADR-003 · ADR-017). Immutable after create.';

COMMENT ON COLUMN billing.invoice.issuer_organization_id IS
    'Logical reference to org.organization (issuer). No FK by design (ADR-013). Always required.';

COMMENT ON COLUMN billing.invoice.bill_to_patient_id IS
    'Logical reference to patient.patient (bill-to). No FK by design (ADR-013). Exactly one of bill_to_patient_id / bill_to_organization_id is present.';

COMMENT ON COLUMN billing.invoice.bill_to_organization_id IS
    'Logical reference to org.organization (B2B bill-to). No FK by design (ADR-013). Must differ from issuer_organization_id.';

COMMENT ON COLUMN billing.invoice.invoice_number IS
    'Optional human-facing invoice number. Soft-unique per tenant when present (partial unique index).';

COMMENT ON COLUMN billing.invoice.status IS
    'Lifecycle state aligned with InvoiceStatus enum: DRAFT | ISSUED | VOIDED. No PAID; no physical delete; no un-void.';

COMMENT ON TABLE billing.invoice_line IS
    'InvoiceLine internal entity — always persisted/read together with its owning Invoice (ADR-017 §8/§9). No quantity, tax, Appointment, or Stock semantics.';

COMMENT ON COLUMN billing.invoice_line.item_id IS
    'Optional logical reference to inventory.item. No FK by design (ADR-013).';

COMMENT ON COLUMN billing.invoice_line.encounter_id IS
    'Optional logical reference to encounter.encounter. No FK by design (ADR-013). When the Invoice bill-to is a Patient, the encounter patientId must match it (validated in the application layer).';
