package com.codecore.iam.domain.exception;

/**
 * Raised when a value object cannot be constructed with valid domain semantics.
 */
public class InvalidDomainValueException extends IamDomainException {

    public InvalidDomainValueException(String message) {
        super(message);
    }
}
