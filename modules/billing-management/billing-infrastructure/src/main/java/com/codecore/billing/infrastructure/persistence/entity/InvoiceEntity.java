package com.codecore.billing.infrastructure.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * R2DBC row mapping for {@code billing.invoice}.
 */
@Table(name = "invoice", schema = "billing")
public class InvoiceEntity implements Persistable<UUID> {

    @Transient
    private boolean newEntity;

    @Id
    @Column("invoice_id")
    private UUID invoiceId;

    @Column("tenant_id")
    private UUID tenantId;

    @Column("issuer_organization_id")
    private UUID issuerOrganizationId;

    @Column("bill_to_patient_id")
    private UUID billToPatientId;

    @Column("bill_to_organization_id")
    private UUID billToOrganizationId;

    @Column("invoice_number")
    private String invoiceNumber;

    private String currency;

    private String status;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    public InvoiceEntity() {
    }

    @Override
    public UUID getId() {
        return invoiceId;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    public void setNewEntity(boolean newEntity) {
        this.newEntity = newEntity;
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

    public UUID getIssuerOrganizationId() {
        return issuerOrganizationId;
    }

    public void setIssuerOrganizationId(UUID issuerOrganizationId) {
        this.issuerOrganizationId = issuerOrganizationId;
    }

    public UUID getBillToPatientId() {
        return billToPatientId;
    }

    public void setBillToPatientId(UUID billToPatientId) {
        this.billToPatientId = billToPatientId;
    }

    public UUID getBillToOrganizationId() {
        return billToOrganizationId;
    }

    public void setBillToOrganizationId(UUID billToOrganizationId) {
        this.billToOrganizationId = billToOrganizationId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
