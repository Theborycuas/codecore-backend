package com.codecore.encounter.domain.exception;

/**
 * Raised when StaffAssignment ↔ Organization / Office coherence fails,
 * or Appointment.patientId ≠ Encounter.patientId (ADR-015 §7).
 */
public final class EncounterCoherenceException extends EncounterDomainException {

    public EncounterCoherenceException(String message) {
        super(message);
    }
}
