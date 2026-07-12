package com.codecore.inventory.contract;

/**
 * Inventory contract surface for cross-BC consumers (ADR-016 / ADR-013).
 * <p>
 * Published surface (FASE 20.5):
 * <ul>
 *   <li>{@link com.codecore.inventory.domain.valueobject.ItemId} (via {@code api} on inventory-domain)</li>
 *   <li>{@link com.codecore.inventory.contract.authorization.ItemPermissionCatalog}</li>
 * </ul>
 * <p>
 * Deferred to closeout (20.8):
 * <ul>
 *   <li>{@code ItemReferencePort}</li>
 * </ul>
 * Consumers depend on {@code inventory-contract} only — never inventory-application
 * or inventory-infrastructure.
 */
public final class InventoryContractMarker {

    private InventoryContractMarker() {
    }
}
