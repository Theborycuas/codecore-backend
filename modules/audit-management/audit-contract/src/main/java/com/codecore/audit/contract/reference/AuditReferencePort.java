package com.codecore.audit.contract.reference;

import com.codecore.audit.domain.valueobject.AuditEntryId;
import com.codecore.audit.domain.valueobject.TenantId;
import reactor.core.publisher.Mono;

/**
 * Cross-BC reference contract for AuditEntry (ADR-013 / ADR-020).
 * Read-only; minimal surface for future consumers.
 */
public interface AuditReferencePort {

    /**
     * {@code true} when the audit entry exists in the given tenant.
     */
    Mono<Boolean> existsByIdAndTenant(AuditEntryId auditEntryId, TenantId tenantId);
}
