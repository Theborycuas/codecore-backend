package com.codecore.billing.domain.exception;

/**
 * Raised when a value object cannot be constructed with valid domain semantics.
 */
public final class InvalidDomainValueException extends InvoiceDomainException {

    public InvalidDomainValueException(String message) {
        super(message);
    }
}
