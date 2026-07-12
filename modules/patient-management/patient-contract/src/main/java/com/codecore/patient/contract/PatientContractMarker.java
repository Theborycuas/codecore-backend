package com.codecore.patient.contract;

/**
 * Clinical Foundation contract surface for cross-BC consumers (ADR-012 / ADR-013).
 * <p>
 * Published surface:
 * <ul>
 *   <li>{@link com.codecore.patient.domain.valueobject.PatientId} (via {@code api} on patient-domain)</li>
 *   <li>{@link com.codecore.patient.contract.authorization.PatientPermissionCatalog}</li>
 *   <li>{@link com.codecore.patient.contract.reference.PatientReferencePort}</li>
 * </ul>
 * Consumers depend on {@code patient-contract} only — never patient-application or patient-infrastructure.
 */
public final class PatientContractMarker {

    private PatientContractMarker() {
    }
}
