package com.codecore.payment.application.query;

/**
 * Status filter for Payment administration listing. Defaults to {@code RECORDED} — the
 * effective settlement work queue (ADR-018 §HTTP — default list status=RECORDED).
 */
public enum PaymentListFilter {
    RECORDED,
    VOIDED,
    ALL;

    public static PaymentListFilter parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return RECORDED;
        }
        return PaymentListFilter.valueOf(raw.trim().toUpperCase());
    }
}
