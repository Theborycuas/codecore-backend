package com.codecore.organization.domain.exception;

/**
 * Raised when a value object cannot be constructed with valid domain semantics.
 */
public final class InvalidDomainValueException extends OrganizationDomainException {

    public InvalidDomainValueException(String message) {
        super(message);
    }
}
