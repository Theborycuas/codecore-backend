package com.codecore.audit.application.query;

import java.util.UUID;

/**
 * Optional list filters for admin audit queries (FASE 24.6) — no status filter (append-only).
 */
public record AuditListQuery(
        String actionCode,
        String resourceType,
        UUID resourceId
) {

    public static AuditListQuery of(String actionCode, String resourceType, UUID resourceId) {
        return new AuditListQuery(
                blankToNull(actionCode),
                blankToNull(resourceType),
                resourceId
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
