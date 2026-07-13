package com.codecore.billing.infrastructure.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

/**
 * R2DBC row mapping for {@code billing.invoice_line}.
 * Always persisted as a fresh insert — {@link com.codecore.billing.infrastructure.persistence.repository.R2dbcInvoiceRepository}
 * fully replaces the line set of an Invoice on every save (ADR-017 §8/§9 — internal entities, no partial line updates).
 */
@Table(name = "invoice_line", schema = "billing")
public class InvoiceLineEntity implements Persistable<UUID> {

    @Transient
    private boolean newEntity = true;

    @Id
    @Column("line_id")
    private UUID lineId;

    @Column("invoice_id")
    private UUID invoiceId;

    @Column("tenant_id")
    private UUID tenantId;

    private String description;

    private String currency;

    @Column("amount_minor")
    private long amountMinor;

    @Column("item_id")
    private UUID itemId;

    @Column("encounter_id")
    private UUID encounterId;

    public InvoiceLineEntity() {
    }

    @Override
    public UUID getId() {
        return lineId;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    public void setNewEntity(boolean newEntity) {
        this.newEntity = newEntity;
    }

    public UUID getLineId() {
        return lineId;
    }

    public void setLineId(UUID lineId) {
        this.lineId = lineId;
    }

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(UUID invoiceId) {
        this.invoiceId = invoiceId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public long getAmountMinor() {
        return amountMinor;
    }

    public void setAmountMinor(long amountMinor) {
        this.amountMinor = amountMinor;
    }

    public UUID getItemId() {
        return itemId;
    }

    public void setItemId(UUID itemId) {
        this.itemId = itemId;
    }

    public UUID getEncounterId() {
        return encounterId;
    }

    public void setEncounterId(UUID encounterId) {
        this.encounterId = encounterId;
    }
}
