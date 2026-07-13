package com.codecore.billing.interfaces.http.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateInvoiceRequest(
        @NotNull UUID issuerOrganizationId,
        UUID billToPatientId,
        UUID billToOrganizationId,
        @Size(max = 64) String invoiceNumber,
        @NotNull @Size(min = 3, max = 3) String currency,
        @NotEmpty @Valid List<InvoiceLineRequest> lines
) {
}
