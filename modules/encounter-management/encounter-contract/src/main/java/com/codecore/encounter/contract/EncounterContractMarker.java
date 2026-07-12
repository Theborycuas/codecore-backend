package com.codecore.encounter.contract;

/**
 * Clinical Records contract surface for cross-BC consumers (ADR-015 / ADR-013).
 * <p>
 * Published surface:
 * <ul>
 *   <li>{@link com.codecore.encounter.domain.valueobject.EncounterId} (via {@code api} on encounter-domain)</li>
 *   <li>{@link com.codecore.encounter.contract.authorization.EncounterPermissionCatalog}</li>
 *   <li>{@link com.codecore.encounter.contract.reference.EncounterReferencePort}</li>
 *   <li>{@link com.codecore.encounter.contract.reference.EncounterReferenceView}</li>
 * </ul>
 * Consumers depend on {@code encounter-contract} only — never encounter-application
 * or encounter-infrastructure.
 */
public final class EncounterContractMarker {

    private EncounterContractMarker() {
    }
}
