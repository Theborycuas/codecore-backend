package com.codecore.patient.domain.exception;

/**
 * Raised when a value object cannot be constructed with valid domain semantics.
 */
public final class InvalidDomainValueException extends PatientDomainException {

    public InvalidDomainValueException(String message) {
        super(message);
    }
}
