-- =============================================================================
-- V6 — Unique tenant name (CreateTenant duplicate guard at DB level)
-- =============================================================================

ALTER TABLE iam.tenant
    ADD CONSTRAINT uq_tenant_name UNIQUE (name);
