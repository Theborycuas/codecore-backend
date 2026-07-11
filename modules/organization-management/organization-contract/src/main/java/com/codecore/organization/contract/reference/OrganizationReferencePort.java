package com.codecore.organization.contract.reference;

import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.organization.domain.valueobject.TenantId;
import reactor.core.publisher.Mono;

/**
 * Cross-BC reference contract for Organization (ADR-013).
 * Read-only; minimal surface for consumer write-time invariants.
 */
public interface OrganizationReferencePort {

    /**
     * {@code true} when the organization exists in the tenant and status is {@code ACTIVE}.
     */
    Mono<Boolean> existsActiveByIdAndTenant(OrganizationId organizationId, TenantId tenantId);
}
