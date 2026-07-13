package com.codecore.billing.interfaces.http.admin.dto;

import com.codecore.billing.application.dto.AdminInvoiceView;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        UUID tenantId,
        UUID issuerOrganizationId,
        UUID billToPatientId,
        UUID billToOrganizationId,
        String invoiceNumber,
        String currency,
        List<InvoiceLineResponse> lines,
        long totalAmountMinor,
        String status,
        Instant createdAt,
        Instant updatedAt
) {

    public static InvoiceResponse from(AdminInvoiceView view) {
        return new InvoiceResponse(
                view.id().value(),
                view.tenantId().value(),
                view.issuerOrganizationUuid(),
                view.billToPatientUuid(),
                view.billToOrganizationUuid(),
                view.invoiceNumber(),
                view.currency(),
                view.lines().stream().map(InvoiceLineResponse::from).toList(),
                view.totalAmountMinor(),
                view.status().name(),
                view.createdAt(),
                view.updatedAt()
        );
    }
}
