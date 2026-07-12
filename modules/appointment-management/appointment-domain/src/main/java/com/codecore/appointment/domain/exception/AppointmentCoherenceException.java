package com.codecore.appointment.domain.exception;

/**
 * Raised when StaffAssignment ↔ Organization / Office coherence fails (ADR-014 §7).
 */
public final class AppointmentCoherenceException extends AppointmentDomainException {

    public AppointmentCoherenceException(String message) {
        super(message);
    }
}
