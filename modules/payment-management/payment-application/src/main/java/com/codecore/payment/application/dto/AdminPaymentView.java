package com.codecore.payment.application.dto;

import com.codecore.payment.domain.valueobject.InvoiceId;
import com.codecore.payment.domain.valueobject.PaymentId;
import com.codecore.payment.domain.valueobject.PaymentStatus;
import com.codecore.payment.domain.valueobject.TenantId;

import java.time.Instant;
import java.util.UUID;

public record AdminPaymentView(
        PaymentId id,
        TenantId tenantId,
        InvoiceId invoiceId,
        String currency,
        long amountMinor,
        String paymentMethodCode,
        Instant recordedAt,
        PaymentStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public UUID invoiceUuid() {
        return invoiceId.value();
    }
}
