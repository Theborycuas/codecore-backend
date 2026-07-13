package com.codecore.access.domain.exception;

/**
 * Raised when a value object cannot be constructed with valid domain semantics.
 */
public final class InvalidDomainValueException extends AccessDomainException {

    public InvalidDomainValueException(String message) {
        super(message);
    }
}
