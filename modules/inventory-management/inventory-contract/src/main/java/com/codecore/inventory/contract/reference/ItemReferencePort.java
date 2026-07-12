package com.codecore.inventory.contract.reference;

import com.codecore.inventory.domain.valueobject.ItemId;
import com.codecore.inventory.domain.valueobject.TenantId;
import reactor.core.publisher.Mono;

/**
 * Cross-BC reference contract for Item (ADR-013 / ADR-016).
 * Read-only; minimal surface for consumer write-time invariants (Stock, Billing, clinical consumption).
 */
public interface ItemReferencePort {

    /**
     * {@code true} when the item exists in the tenant and status is {@code ACTIVE}.
     */
    Mono<Boolean> existsActiveByIdAndTenant(ItemId itemId, TenantId tenantId);
}
