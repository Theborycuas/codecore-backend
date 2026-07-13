package com.codecore.payment.domain.exception;

/**
 * Raised when a value object cannot be constructed with valid domain semantics.
 */
public final class InvalidDomainValueException extends PaymentDomainException {

    public InvalidDomainValueException(String message) {
        super(message);
    }
}
