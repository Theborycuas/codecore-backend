package com.codecore.appointment.domain.exception;

/**
 * Raised when an Appointment cannot be resolved for the requested identity/tenant scope.
 */
public final class AppointmentNotFoundException extends AppointmentDomainException {

    public AppointmentNotFoundException(String message) {
        super(message);
    }
}
