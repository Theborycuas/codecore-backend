package com.codecore.payment.domain.exception;

/**
 * Raised when a payment lifecycle transition is not allowed.
 */
public final class InvalidPaymentStateException extends PaymentDomainException {

    public InvalidPaymentStateException(String message) {
        super(message);
    }
}
