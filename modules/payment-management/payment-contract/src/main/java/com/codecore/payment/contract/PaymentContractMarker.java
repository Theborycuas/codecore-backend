package com.codecore.payment.contract;

/**
 * Payments contract surface for cross-BC consumers (ADR-018 / ADR-013).
 * <p>
 * Published surface (FASE 22 closed — Payment slice):
 * <ul>
 *   <li>{@link com.codecore.payment.domain.valueobject.PaymentId} (via {@code api} on payment-domain)</li>
 *   <li>{@link com.codecore.payment.contract.authorization.PaymentPermissionCatalog}</li>
 *   <li>{@link com.codecore.payment.contract.reference.PaymentReferencePort}</li>
 * </ul>
 * Consumers depend on {@code payment-contract} only — never payment-application
 * or payment-infrastructure.
 */
public final class PaymentContractMarker {

    private PaymentContractMarker() {
    }
}
