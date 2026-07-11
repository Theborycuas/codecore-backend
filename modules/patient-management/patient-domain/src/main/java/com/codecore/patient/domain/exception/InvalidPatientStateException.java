package com.codecore.patient.domain.exception;

/**
 * Raised when a patient lifecycle or mutation transition is not allowed.
 */
public final class InvalidPatientStateException extends PatientDomainException {

    public InvalidPatientStateException(String message) {
        super(message);
    }
}
