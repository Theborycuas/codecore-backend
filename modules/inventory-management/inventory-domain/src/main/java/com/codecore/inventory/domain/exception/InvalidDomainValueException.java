package com.codecore.inventory.domain.exception;

/**
 * Raised when a value object cannot be constructed with valid domain semantics.
 */
public final class InvalidDomainValueException extends ItemDomainException {

    public InvalidDomainValueException(String message) {
        super(message);
    }
}
