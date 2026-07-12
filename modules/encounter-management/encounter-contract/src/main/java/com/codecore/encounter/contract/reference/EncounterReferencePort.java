package com.codecore.encounter.contract.reference;

import com.codecore.encounter.domain.valueobject.EncounterId;
import com.codecore.encounter.domain.valueobject.TenantId;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * Cross-BC reference contract for Encounter (ADR-013 / ADR-015).
 * Read-only; minimal surface for consumer write-time invariants
 * (ClinicalDocument / Notes, Labs, Billing, Consent, …).
 */
public interface EncounterReferencePort {

    /**
     * {@code true} when the encounter exists in the tenant and status is {@code IN_PROGRESS}
     * (open episode — default for attaching live clinical documentation).
     * <p>
     * {@code CANCELLED} and {@code COMPLETED} return {@code false}.
     */
    Mono<Boolean> existsInProgressByIdAndTenant(EncounterId encounterId, TenantId tenantId);

    /**
     * Minimal {@code patientId} + {@code status} when the encounter exists in the tenant
     * and is linkable for Notes / Labs / Billing (status ∈ {@code IN_PROGRESS}, {@code COMPLETED}).
     * Empty for unknown id, wrong tenant, or {@code CANCELLED}.
     */
    Mono<Optional<EncounterReferenceView>> findLinkableByIdAndTenant(
            EncounterId encounterId,
            TenantId tenantId
    );
}
