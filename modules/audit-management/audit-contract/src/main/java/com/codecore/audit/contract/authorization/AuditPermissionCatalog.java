package com.codecore.audit.contract.authorization;

import java.util.Set;

/**
 * Audit (AuditEntry) permission contract (ADR-020 / FASE 24.5).
 * <p>
 * String codes are the canonical identifiers seeded in {@code iam.permission}.
 * IAM maps them to {@code PermissionCode} via {@code IamPermissionCatalog}.
 * <p>
 * Intentionally limited to read — append is via {@code AuditAppendPort}, not HTTP write
 * permissions ({@code audit:update} / {@code audit:delete} are out of scope).
 */
public final class AuditPermissionCatalog {

    public static final String AUDIT_READ = "audit:read";

    /** Full Audit query contract (FASE 24 — read only). */
    public static final Set<String> ALL = Set.of(AUDIT_READ);

    /** Read-only consultation of audit entries. */
    public static final Set<String> AUDIT_READ_ONLY = ALL;

    private AuditPermissionCatalog() {
    }
}
