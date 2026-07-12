package com.codecore.organization.contract.reference;

import com.codecore.organization.domain.valueobject.OfficeId;
import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.organization.domain.valueobject.TenantId;
import reactor.core.publisher.Mono;

/**
 * Cross-BC reference contract for Office (ADR-013).
 * Read-only; minimal surface for consumer write-time invariants (Appointment, Inventory, …).
 */
public interface OfficeReferencePort {

    /**
     * {@code true} when the office exists in the tenant, status is {@code ACTIVE},
     * and it belongs to the given organization.
     */
    Mono<Boolean> existsActiveInOrganization(
            OfficeId officeId,
            OrganizationId organizationId,
            TenantId tenantId
    );
}
