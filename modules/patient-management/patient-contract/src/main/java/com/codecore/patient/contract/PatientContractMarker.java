package com.codecore.patient.contract;

/**
 * Clinical Foundation contract surface for cross-BC consumers.
 * <p>
 * Today: {@link com.codecore.patient.domain.valueobject.PatientId} is published via
 * {@code api} dependency on {@code patient-domain}.
 * <p>
 * {@code PatientReferencePort} will be added when a downstream BC needs write-time
 * ACTIVE validation (ADR-013) — not in PASO 17.3.
 */
public final class PatientContractMarker {

    private PatientContractMarker() {
    }
}
