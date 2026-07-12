-- =============================================================================
-- V24 — Item (tenant-scoped inventoriable catalog identity)
-- Schema: inventory
-- PostgreSQL 16+
-- FASE 20.4 — Item Persistence (ADR-016 · ADR-003 · ADR-013)
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS inventory;

CREATE TABLE inventory.item (
    item_id                   UUID         NOT NULL,
    tenant_id                 UUID         NOT NULL,
    primary_organization_id   UUID,
    display_name              VARCHAR(200) NOT NULL,
    code                      VARCHAR(64),
    status                    VARCHAR(50)  NOT NULL,
    created_at                TIMESTAMPTZ  NOT NULL,
    updated_at                TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_item
        PRIMARY KEY (item_id),

    CONSTRAINT ck_item_status
        CHECK (status IN (
            'ACTIVE',
            'ARCHIVED'
        )),

    CONSTRAINT ck_item_display_name_not_blank
        CHECK (char_length(trim(display_name)) > 0),

    CONSTRAINT ck_item_code_not_blank
        CHECK (code IS NULL OR char_length(trim(code)) > 0)
);

-- Soft-unique human code per tenant (ADR-016): multiple NULLs allowed.
CREATE UNIQUE INDEX uq_inventory_item_tenant_code
    ON inventory.item (tenant_id, code)
    WHERE code IS NOT NULL;

CREATE INDEX idx_inventory_item_tenant_id
    ON inventory.item (tenant_id);

CREATE INDEX idx_inventory_item_status
    ON inventory.item (status);

CREATE INDEX idx_inventory_item_tenant_status
    ON inventory.item (tenant_id, status);

CREATE INDEX idx_inventory_item_primary_organization_id
    ON inventory.item (tenant_id, primary_organization_id);

COMMENT ON SCHEMA inventory IS
    'Inventory bounded context schema (Item catalog and future Stock / Movement tables).';

COMMENT ON TABLE inventory.item IS
    'Item aggregate root — inventoriable identity / stockable catalog entry (ADR-016). Intentionally small.';

COMMENT ON COLUMN inventory.item.tenant_id IS
    'Logical reference to iam.tenant (domain TenantId). No FK: Inventory stays decoupled from IAM schema lifecycle (ADR-003 · ADR-016).';

COMMENT ON COLUMN inventory.item.primary_organization_id IS
    'Optional PrimaryOrganizationId — logical reference to org.organization. No FK by design (ADR-011 · ADR-013). Not stock ownership.';

COMMENT ON COLUMN inventory.item.code IS
    'Optional human SKU / material code. Soft-unique per tenant when present (partial unique index).';

COMMENT ON COLUMN inventory.item.status IS
    'Lifecycle state aligned with ItemStatus enum: ACTIVE | ARCHIVED.';
