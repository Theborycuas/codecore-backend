package com.codecore.organization.contract.reference;

import com.codecore.organization.domain.valueobject.StaffAssignmentId;
import com.codecore.organization.domain.valueobject.TenantId;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * Cross-BC reference contract for StaffAssignment (ADR-013).
 * Read-only; minimal surface for consumer write-time coherence (e.g. Appointment).
 * <p>
 * StaffAssignment has no ACTIVE/ARCHIVED lifecycle — existence in tenant is the linkability signal.
 * Physical delete revokes scope (ADR-011).
 */
public interface StaffAssignmentReferencePort {

    /**
     * Minimal organization/office scope when the assignment exists in the tenant; empty otherwise.
     */
    Mono<Optional<StaffAssignmentReferenceView>> findScopeByIdAndTenant(
            StaffAssignmentId staffAssignmentId,
            TenantId tenantId
    );
}
