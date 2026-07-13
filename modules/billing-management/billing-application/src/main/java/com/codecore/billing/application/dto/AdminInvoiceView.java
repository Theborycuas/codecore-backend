package com.codecore.billing.application.dto;

import com.codecore.billing.domain.valueobject.BillToOrganizationId;
import com.codecore.billing.domain.valueobject.BillToPatientId;
import com.codecore.billing.domain.valueobject.InvoiceId;
import com.codecore.billing.domain.valueobject.InvoiceStatus;
import com.codecore.billing.domain.valueobject.OrganizationId;
import com.codecore.billing.domain.valueobject.TenantId;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminInvoiceView(
        InvoiceId id,
        TenantId tenantId,
        OrganizationId issuerOrganizationId,
        BillToPatientId billToPatientId,
        BillToOrganizationId billToOrganizationId,
        String invoiceNumber,
        String currency,
        List<AdminInvoiceLineView> lines,
        long totalAmountMinor,
        InvoiceStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public UUID issuerOrganizationUuid() {
        return issuerOrganizationId.value();
    }

    public UUID billToPatientUuid() {
        return billToPatientId == null ? null : billToPatientId.value();
    }

    public UUID billToOrganizationUuid() {
        return billToOrganizationId == null ? null : billToOrganizationId.value();
    }
}
