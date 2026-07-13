package com.codecore.payment.application.query;

import java.util.Objects;
import java.util.UUID;

/**
 * List filters for Payment administration (PASO 22.5.1).
 */
public record PaymentListQuery(
        PaymentListFilter status,
        UUID invoiceId
) {

    public PaymentListQuery {
        status = Objects.requireNonNull(status, "status");
    }

    public static PaymentListQuery of(String status, UUID invoiceId) {
        return new PaymentListQuery(PaymentListFilter.parse(status), invoiceId);
    }
}
