package com.codecore.patient.domain.exception;

/**
 * Base exception for Clinical Foundation (Patient) domain rule violations.
 */
public class PatientDomainException extends RuntimeException {

    public PatientDomainException(String message) {
        super(message);
    }
}
