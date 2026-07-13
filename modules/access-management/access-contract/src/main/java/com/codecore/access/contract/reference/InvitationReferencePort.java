package com.codecore.access.contract.reference;

import com.codecore.access.domain.valueobject.InvitationId;
import com.codecore.access.domain.valueobject.TenantId;
import reactor.core.publisher.Mono;

/**
 * Cross-BC reference contract for Invitation (ADR-013 / ADR-019).
 * Read-only; minimal surface for future consumer write-time invariants.
 */
public interface InvitationReferencePort {

    /**
     * {@code true} when the invitation exists in the tenant and status is {@code PENDING}.
     */
    Mono<Boolean> existsPendingByIdAndTenant(InvitationId invitationId, TenantId tenantId);
}
