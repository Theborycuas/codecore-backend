package com.codecore.payment.domain.exception;

/**
 * Raised when a Payment cannot be found in the current tenant context.
 */
public final class PaymentNotFoundException extends PaymentDomainException {

    public PaymentNotFoundException(String message) {
        super(message);
    }
}
