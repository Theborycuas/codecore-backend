package com.codecore.billing.application.command;

import com.codecore.billing.domain.valueobject.InvoiceId;

import java.util.List;
import java.util.UUID;

/**
 * Full replace of mutable Invoice content (PUT semantics) — only applicable in {@code DRAFT}.
 */
public record UpdateInvoiceCommand(
        InvoiceId invoiceId,
        UUID issuerOrganizationId,
        UUID billToPatientId,
        UUID billToOrganizationId,
        String invoiceNumber,
        String currency,
        List<InvoiceLineDraft> lines
) {
}
