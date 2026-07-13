package com.codecore.payment.interfaces.http.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Payment create request (ADR-018). No client-supplied {@code tenantId} — the tenant is
 * always resolved from the authenticated JWT context, never from client input.
 */
public record CreatePaymentRequest(
        @NotNull UUID invoiceId,
        @NotNull @Size(min = 3, max = 3) String currency,
        @Positive long amountMinor,
        @Size(max = 32) String paymentMethodCode
) {
}
