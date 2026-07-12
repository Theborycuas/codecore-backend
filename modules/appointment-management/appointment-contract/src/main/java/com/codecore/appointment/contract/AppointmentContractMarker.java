package com.codecore.appointment.contract;

/**
 * Scheduling contract surface for cross-BC consumers (ADR-014 / ADR-013).
 * <p>
 * Published surface in PASO 18.3:
 * <ul>
 *   <li>{@link com.codecore.appointment.domain.valueobject.AppointmentId} (via {@code api} on appointment-domain)</li>
 * </ul>
 * {@code AppointmentReferencePort} is deferred to a later PASO. Consumers depend on
 * {@code appointment-contract} only — never appointment-application or appointment-infrastructure.
 */
public final class AppointmentContractMarker {

    private AppointmentContractMarker() {
    }
}
