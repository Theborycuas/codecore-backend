package com.codecore.billing.infrastructure.persistence.mapper;

import com.codecore.billing.domain.model.invoice.Invoice;
import com.codecore.billing.domain.model.invoice.InvoiceLine;
import com.codecore.billing.domain.valueobject.BillTo;
import com.codecore.billing.domain.valueobject.BillToOrganizationId;
import com.codecore.billing.domain.valueobject.BillToPatientId;
import com.codecore.billing.domain.valueobject.EncounterId;
import com.codecore.billing.domain.valueobject.InvoiceId;
import com.codecore.billing.domain.valueobject.InvoiceLineId;
import com.codecore.billing.domain.valueobject.InvoiceNumber;
import com.codecore.billing.domain.valueobject.InvoiceStatus;
import com.codecore.billing.domain.valueobject.ItemId;
import com.codecore.billing.domain.valueobject.LineDescription;
import com.codecore.billing.domain.valueobject.Money;
import com.codecore.billing.domain.valueobject.OrganizationId;
import com.codecore.billing.domain.valueobject.TenantId;
import com.codecore.billing.infrastructure.persistence.entity.InvoiceEntity;
import com.codecore.billing.infrastructure.persistence.entity.InvoiceLineEntity;

import java.util.List;

/**
 * Isomorphic mapping between {@link InvoiceEntity} / {@link InvoiceLineEntity} and the
 * {@link Invoice} aggregate (ADR-017). Invoice + its lines are always persisted/read together
 * as a single unit — there is no independent InvoiceLine repository.
 */
public final class InvoiceMapper {

    public Invoice toDomain(InvoiceEntity entity, List<InvoiceLineEntity> lineEntities) {
        BillToPatientId billToPatientId = entity.getBillToPatientId() == null
                ? null
                : BillToPatientId.of(entity.getBillToPatientId());
        BillToOrganizationId billToOrganizationId = entity.getBillToOrganizationId() == null
                ? null
                : BillToOrganizationId.of(entity.getBillToOrganizationId());
        BillTo billTo = BillTo.of(billToPatientId, billToOrganizationId);

        List<InvoiceLine> lines = lineEntities.stream()
                .map(this::toLineDomain)
                .toList();

        return Invoice.reconstitute(
                new InvoiceId(entity.getInvoiceId()),
                new TenantId(entity.getTenantId()),
                OrganizationId.of(entity.getIssuerOrganizationId()),
                billTo,
                entity.getInvoiceNumber() == null ? null : InvoiceNumber.of(entity.getInvoiceNumber()),
                lines,
                InvoiceStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public InvoiceEntity toEntity(Invoice invoice, boolean isNew) {
        InvoiceEntity entity = new InvoiceEntity();
        entity.setNewEntity(isNew);
        entity.setInvoiceId(invoice.id().value());
        entity.setTenantId(invoice.tenantId().value());
        entity.setIssuerOrganizationId(invoice.issuerOrganizationId().value());
        entity.setBillToPatientId(invoice.billTo().patientId().map(BillToPatientId::value).orElse(null));
        entity.setBillToOrganizationId(
                invoice.billTo().organizationId().map(BillToOrganizationId::value).orElse(null)
        );
        entity.setInvoiceNumber(invoice.invoiceNumber().map(InvoiceNumber::value).orElse(null));
        entity.setCurrency(invoice.currency());
        entity.setStatus(invoice.status().name());
        entity.setCreatedAt(invoice.createdAt());
        entity.setUpdatedAt(invoice.updatedAt());
        return entity;
    }

    public List<InvoiceLineEntity> toLineEntities(Invoice invoice) {
        return invoice.lines().stream()
                .map(line -> toLineEntity(invoice, line))
                .toList();
    }

    private InvoiceLineEntity toLineEntity(Invoice invoice, InvoiceLine line) {
        InvoiceLineEntity entity = new InvoiceLineEntity();
        entity.setNewEntity(true);
        entity.setLineId(line.id().value());
        entity.setInvoiceId(invoice.id().value());
        entity.setTenantId(invoice.tenantId().value());
        entity.setDescription(line.description().value());
        entity.setCurrency(line.amount().currency());
        entity.setAmountMinor(line.amount().amountMinor());
        entity.setItemId(line.itemId().map(ItemId::value).orElse(null));
        entity.setEncounterId(line.encounterId().map(EncounterId::value).orElse(null));
        return entity;
    }

    private InvoiceLine toLineDomain(InvoiceLineEntity entity) {
        return InvoiceLine.reconstitute(
                new InvoiceLineId(entity.getLineId()),
                LineDescription.of(entity.getDescription()),
                Money.of(entity.getCurrency(), entity.getAmountMinor()),
                entity.getItemId() == null ? null : ItemId.of(entity.getItemId()),
                entity.getEncounterId() == null ? null : EncounterId.of(entity.getEncounterId())
        );
    }
}
