-- =============================================================================
-- V28 — Payment (settlement record aggregate)
-- Schema: payments
-- PostgreSQL 16+
-- FASE 22.4 — Payment Persistence (ADR-018 · ADR-003 · ADR-013)
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS payments;

CREATE TABLE payments.payment (
    payment_id              UUID          NOT NULL,
    tenant_id                UUID          NOT NULL,
    invoice_id                UUID          NOT NULL,
    currency                 VARCHAR(3)    NOT NULL,
    amount_minor              BIGINT        NOT NULL,
    payment_method_code       VARCHAR(32),
    recorded_at                TIMESTAMPTZ   NOT NULL,
    status                    VARCHAR(20)   NOT NULL,
    created_at                 TIMESTAMPTZ   NOT NULL,
    updated_at                 TIMESTAMPTZ   NOT NULL,

    CONSTRAINT pk_payment
        PRIMARY KEY (payment_id),

    CONSTRAINT ck_payment_status
        CHECK (status IN (
            'RECORDED',
            'VOIDED'
        )),

    CONSTRAINT ck_payment_amount_positive
        CHECK (amount_minor > 0),

    CONSTRAINT ck_payment_currency_iso
        CHECK (currency ~ '^[A-Z]{3}$'),

    CONSTRAINT ck_payment_method_code_not_blank
        CHECK (payment_method_code IS NULL OR char_length(trim(payment_method_code)) > 0)
);

-- No FK to billing.invoice by design (ADR-013 / ADR-018): Payments stays decoupled from
-- Billing schema lifecycle. Existence + ISSUED status is validated at write time via
-- InvoiceReferencePort, not enforced at the database level.

CREATE INDEX idx_payments_payment_tenant_id
    ON payments.payment (tenant_id);

CREATE INDEX idx_payments_payment_status
    ON payments.payment (status);

CREATE INDEX idx_payments_payment_tenant_status
    ON payments.payment (tenant_id, status);

CREATE INDEX idx_payments_payment_invoice_id
    ON payments.payment (tenant_id, invoice_id);

COMMENT ON SCHEMA payments IS
    'Payments bounded context schema (Payment settlement-record aggregate — ADR-018).';

COMMENT ON TABLE payments.payment IS
    'Payment aggregate root — the settlement record of an amount paid against an Invoice under a Tenant (ADR-018). Intentionally small: no refunds, ledger postings, PSP capture state, or Invoice mutation.';

COMMENT ON COLUMN payments.payment.tenant_id IS
    'Logical reference to iam.tenant (domain TenantId). No FK: Payments stays decoupled from IAM schema lifecycle (ADR-003 · ADR-018). Immutable after create.';

COMMENT ON COLUMN payments.payment.invoice_id IS
    'Logical reference to billing.invoice. No FK by design (ADR-013 · ADR-018). Existence + ISSUED status validated at write time via InvoiceReferencePort. Immutable after create.';

COMMENT ON COLUMN payments.payment.payment_method_code IS
    'Optional opaque payment method label (e.g. CASH, CARD, WIRE_TRANSFER, or a PSP-defined code), capped at 32 chars. Not interpreted or validated against a fixed enum.';

COMMENT ON COLUMN payments.payment.status IS
    'Lifecycle state aligned with PaymentStatus enum: RECORDED | VOIDED. No DRAFT; no content update; no physical delete; no un-void; no PAID on Invoice.';
