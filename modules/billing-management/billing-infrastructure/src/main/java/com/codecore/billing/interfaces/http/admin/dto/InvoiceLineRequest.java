package com.codecore.billing.interfaces.http.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record InvoiceLineRequest(
        @NotBlank @Size(max = 500) String description,
        @Positive long amountMinor,
        UUID itemId,
        UUID encounterId
) {
}
