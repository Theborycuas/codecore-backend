package com.codecore.appointment.domain.exception;

/**
 * Raised when a cross-BC reference is missing or not usable for scheduling writes (ADR-013).
 */
public final class AppointmentReferenceNotFoundException extends AppointmentDomainException {

    public AppointmentReferenceNotFoundException(String message) {
        super(message);
    }
}
