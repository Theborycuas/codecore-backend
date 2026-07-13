package com.codecore.payment.interfaces.http.admin.dto;

import com.codecore.payment.application.dto.AdminPaymentView;

import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID tenantId,
        UUID invoiceId,
        String currency,
        long amountMinor,
        String paymentMethodCode,
        Instant recordedAt,
        String status,
        Instant createdAt,
        Instant updatedAt
) {

    public static PaymentResponse from(AdminPaymentView view) {
        return new PaymentResponse(
                view.id().value(),
                view.tenantId().value(),
                view.invoiceUuid(),
                view.currency(),
                view.amountMinor(),
                view.paymentMethodCode(),
                view.recordedAt(),
                view.status().name(),
                view.createdAt(),
                view.updatedAt()
        );
    }
}
