package com.codecore.billing.application.query;

/**
 * Status filter for Invoice administration listing. Defaults to {@code DRAFT} — the editable
 * work queue — mirroring the operational-first default used by other administration APIs.
 */
public enum InvoiceListFilter {
    DRAFT,
    ISSUED,
    VOIDED,
    ALL;

    public static InvoiceListFilter parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return DRAFT;
        }
        return InvoiceListFilter.valueOf(raw.trim().toUpperCase());
    }
}
