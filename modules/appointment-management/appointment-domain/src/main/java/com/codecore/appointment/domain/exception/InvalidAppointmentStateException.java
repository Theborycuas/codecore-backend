package com.codecore.appointment.domain.exception;

public final class InvalidAppointmentStateException extends AppointmentDomainException {

    public InvalidAppointmentStateException(String message) {
        super(message);
    }
}
