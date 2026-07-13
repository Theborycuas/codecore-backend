package com.codecore.audit.contract.append;

import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Cross-BC append port for AuditEntry (ADR-013 / ADR-020).
 * Producers (Access Invitation, IAM PasswordReset, …) write via this contract — not via HTTP POST.
 */
public interface AuditAppendPort {

    record AppendAuditCommand(
            UUID tenantId,
            String actionCode,
            UUID actorMembershipIdOrNull,
            String resourceType,
            UUID resourceId,
            String outcomeOrNull,
            Instant occurredAt
    ) {
    }

    /** Persists an append-only audit entry; returns the new {@code audit_entry_id}. */
    Mono<UUID> append(AppendAuditCommand cmd);
}
