package com.codecore.billing.contract.authorization;

import java.util.Set;

/**
 * Billing (Invoice) permission contract (ADR-017 §12 / FASE 21.5).
 * <p>
 * String codes are the canonical identifiers seeded in {@code iam.permission}.
 * IAM maps them to {@code PermissionCode} via {@code IamPermissionCatalog}.
 * <p>
 * Intentionally limited to the commercial-claim lifecycle — no {@code invoice:pay},
 * {@code invoice:post}, or {@code invoice:tax} (those belong to Payments / Accounting / Tax).
 */
public final class InvoicePermissionCatalog {

    public static final String INVOICE_CREATE = "invoice:create";
    public static final String INVOICE_READ = "invoice:read";
    public static final String INVOICE_UPDATE = "invoice:update";
    public static final String INVOICE_ISSUE = "invoice:issue";
    public static final String INVOICE_VOID = "invoice:void";

    /** Full Invoice commercial-claim lifecycle contract (FASE 21 — no payments/tax/GL). */
    public static final Set<String> ALL = Set.of(
            INVOICE_CREATE,
            INVOICE_READ,
            INVOICE_UPDATE,
            INVOICE_ISSUE,
            INVOICE_VOID
    );

    /** Read-only consultation of invoices. */
    public static final Set<String> INVOICE_READ_ONLY = Set.of(INVOICE_READ);

    private InvoicePermissionCatalog() {
    }
}
