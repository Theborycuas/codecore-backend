package com.codecore.access.application.query;

/**
 * Status filter for Invitation administration listing. Defaults to {@code PENDING}.
 */
public enum InvitationListFilter {
    PENDING,
    ACCEPTED,
    REVOKED,
    EXPIRED,
    ALL;

    public static InvitationListFilter parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return PENDING;
        }
        return InvitationListFilter.valueOf(raw.trim().toUpperCase());
    }
}
