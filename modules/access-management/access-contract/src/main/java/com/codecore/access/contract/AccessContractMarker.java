package com.codecore.access.contract;

/**
 * Access contract surface for cross-BC consumers (ADR-019 / ADR-013).
 * <p>
 * Published surface (FASE 23 — Invitation slice):
 * <ul>
 *   <li>{@link com.codecore.access.domain.valueobject.InvitationId} (via {@code api} on access-domain)</li>
 *   <li>{@link com.codecore.access.contract.authorization.InvitationPermissionCatalog}</li>
 *   <li>{@link com.codecore.access.contract.reference.InvitationReferencePort}</li>
 * </ul>
 * Consumers depend on {@code access-contract} only — never access-application
 * or access-infrastructure.
 */
public final class AccessContractMarker {

    private AccessContractMarker() {
    }
}
