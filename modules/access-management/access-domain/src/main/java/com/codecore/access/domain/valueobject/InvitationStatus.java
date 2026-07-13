package com.codecore.access.domain.valueobject;

/**
 * Lifecycle of the Invitation join intent (ADR-019).
 * {@code (create) -> PENDING -> accept -> ACCEPTED | revoke -> REVOKED | expire -> EXPIRED}.
 * No {@code DRAFT}; no un-revoke; no re-accept; no physical delete.
 */
public enum InvitationStatus {
    PENDING,
    ACCEPTED,
    REVOKED,
    EXPIRED
}
