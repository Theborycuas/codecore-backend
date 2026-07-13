package com.codecore.billing.contract;

/**
 * Billing contract surface for cross-BC consumers (ADR-017 / ADR-013).
 * <p>
 * Published surface (FASE 21 in progress — Invoice slice):
 * <ul>
 *   <li>{@link com.codecore.billing.domain.valueobject.InvoiceId} (via {@code api} on billing-domain)</li>
 *   <li>{@link com.codecore.billing.contract.authorization.InvoicePermissionCatalog}</li>
 * </ul>
 * {@code InvoiceReferencePort} is published at closeout (PASO 21.8).
 * Consumers depend on {@code billing-contract} only — never billing-application
 * or billing-infrastructure.
 */
public final class BillingContractMarker {

    private BillingContractMarker() {
    }
}
