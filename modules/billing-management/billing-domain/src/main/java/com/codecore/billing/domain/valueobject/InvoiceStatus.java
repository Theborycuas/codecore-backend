package com.codecore.billing.domain.valueobject;

/**
 * Lifecycle of the Invoice commercial claim (ADR-017).
 * {@code (create) -> DRAFT -> issue -> ISSUED -> void -> VOIDED} (also {@code DRAFT -> void -> VOIDED}).
 * No {@code PAID} — that belongs to Payments. No physical delete; no un-void.
 */
public enum InvoiceStatus {
    DRAFT,
    ISSUED,
    VOIDED
}
