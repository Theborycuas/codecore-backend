package com.codecore.appointment.contract;

/**
 * Scheduling contract surface for cross-BC consumers (ADR-014 / ADR-013).
 * <p>
 * Published surface:
 * <ul>
 *   <li>{@link com.codecore.appointment.domain.valueobject.AppointmentId} (via {@code api} on appointment-domain)</li>
 *   <li>{@link com.codecore.appointment.contract.authorization.AppointmentPermissionCatalog}</li>
 *   <li>{@link com.codecore.appointment.contract.reference.AppointmentReferencePort}</li>
 * </ul>
 * Consumers depend on {@code appointment-contract} only — never appointment-application
 * or appointment-infrastructure.
 */
public final class AppointmentContractMarker {

    private AppointmentContractMarker() {
    }
}
