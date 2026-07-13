package com.codecore.payment.contract.authorization;

import java.util.Set;

/**
 * Payments (Payment) permission contract (ADR-018 §12 / FASE 22.5).
 * <p>
 * String codes are the canonical identifiers seeded in {@code iam.permission}.
 * IAM maps them to {@code PermissionCode} via {@code IamPermissionCatalog}.
 * <p>
 * Intentionally limited to the settlement-record lifecycle — no {@code payment:refund},
 * {@code payment:capture}, or {@code payment:post} (those belong to a future Refunds /
 * PSP-capture / Accounting bounded context).
 */
public final class PaymentPermissionCatalog {

    public static final String PAYMENT_CREATE = "payment:create";
    public static final String PAYMENT_READ = "payment:read";
    public static final String PAYMENT_VOID = "payment:void";

    /** Full Payment settlement-record lifecycle contract (FASE 22 — no refund/capture/ledger). */
    public static final Set<String> ALL = Set.of(
            PAYMENT_CREATE,
            PAYMENT_READ,
            PAYMENT_VOID
    );

    /** Read-only consultation of payments. */
    public static final Set<String> PAYMENT_READ_ONLY = Set.of(PAYMENT_READ);

    private PaymentPermissionCatalog() {
    }
}
