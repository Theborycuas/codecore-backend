package com.codecore.appointment.domain.exception;

/**
 * Base exception for Scheduling (Appointment) domain rule violations.
 */
public class AppointmentDomainException extends RuntimeException {

    public AppointmentDomainException(String message) {
        super(message);
    }
}
