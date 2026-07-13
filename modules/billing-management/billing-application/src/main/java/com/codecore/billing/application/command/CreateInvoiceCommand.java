package com.codecore.billing.application.command;

import java.util.List;
import java.util.UUID;

public record CreateInvoiceCommand(
        UUID issuerOrganizationId,
        UUID billToPatientId,
        UUID billToOrganizationId,
        String invoiceNumber,
        String currency,
        List<InvoiceLineDraft> lines
) {
}
