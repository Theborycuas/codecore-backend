package com.codecore.payment.domain.valueobject;

/**
 * Lifecycle of the Payment settlement record (ADR-018).
 * {@code (create) -> RECORDED -> void -> VOIDED}. No {@code DRAFT}; no content update; no
 * physical delete; no un-void.
 */
public enum PaymentStatus {
    RECORDED,
    VOIDED
}
