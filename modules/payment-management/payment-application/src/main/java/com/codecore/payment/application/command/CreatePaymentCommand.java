package com.codecore.payment.application.command;

import java.util.UUID;

public record CreatePaymentCommand(
        UUID invoiceId,
        String currency,
        long amountMinor,
        String paymentMethodCode
) {
}
