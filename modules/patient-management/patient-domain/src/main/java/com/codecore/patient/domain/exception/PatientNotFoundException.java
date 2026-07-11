package com.codecore.patient.domain.exception;

/**
 * Raised when a Patient cannot be found in the current tenant context.
 */
public final class PatientNotFoundException extends PatientDomainException {

    public PatientNotFoundException(String message) {
        super(message);
    }
}
