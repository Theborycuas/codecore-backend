package com.codecore.encounter.contract;

/**
 * Clinical Records contract surface for cross-BC consumers (ADR-015 / ADR-013).
 * <p>
 * Published surface:
 * <ul>
 *   <li>{@link com.codecore.encounter.domain.valueobject.EncounterId} (via {@code api} on encounter-domain)</li>
 * </ul>
 * {@code EncounterReferencePort} is deferred to closeout. Consumers depend on
 * {@code encounter-contract} only — never encounter-application or encounter-infrastructure.
 */
public final class EncounterContractMarker {

    private EncounterContractMarker() {
    }
}
