package com.codecore.payment.contract.reference;

import com.codecore.payment.domain.valueobject.PaymentId;
import com.codecore.payment.domain.valueobject.TenantId;
import reactor.core.publisher.Mono;

/**
 * Cross-BC reference contract for Payment (ADR-013 / ADR-018).
 * Read-only; minimal surface for future consumer write-time invariants (Refunds, reporting).
 */
public interface PaymentReferencePort {

    /**
     * {@code true} when the payment exists in the tenant and status is {@code RECORDED}.
     * <p>
     * {@code VOIDED} returns {@code false} — future consumers (e.g. Refunds) must only act
     * against an effective settlement record (ADR-018 closeout 22.8).
     */
    Mono<Boolean> existsRecordedByIdAndTenant(PaymentId paymentId, TenantId tenantId);
}
