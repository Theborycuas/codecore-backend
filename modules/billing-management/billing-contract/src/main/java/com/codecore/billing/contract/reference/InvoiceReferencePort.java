package com.codecore.billing.contract.reference;

import com.codecore.billing.domain.valueobject.InvoiceId;
import com.codecore.billing.domain.valueobject.TenantId;
import reactor.core.publisher.Mono;

/**
 * Cross-BC reference contract for Invoice (ADR-013 / ADR-017).
 * Read-only; minimal surface for consumer write-time invariants (Payments, CreditNotes, reporting).
 */
public interface InvoiceReferencePort {

    /**
     * {@code true} when the invoice exists in the tenant and status is {@code ISSUED}.
     * <p>
     * {@code DRAFT} and {@code VOIDED} return {@code false} — Payments must only settle
     * an effective commercial claim (ADR-017 §9 / closeout 21.8).
     */
    Mono<Boolean> existsIssuedByIdAndTenant(InvoiceId invoiceId, TenantId tenantId);
}
