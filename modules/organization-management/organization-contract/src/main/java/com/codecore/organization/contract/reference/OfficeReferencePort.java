package com.codecore.organization.contract.reference;

import com.codecore.organization.domain.valueobject.OfficeId;
import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.organization.domain.valueobject.TenantId;
import reactor.core.publisher.Mono;

/**
 * Cross-BC reference contract for Office (ADR-013).
 * Declared for the family; first consumers: Appointment / Inventory (not Patient v1).
 */
public interface OfficeReferencePort {

    /**
     * {@code true} when the office exists in the tenant, is ACTIVE, and belongs to the organization.
     */
    Mono<Boolean> existsActiveInOrganization(
            OfficeId officeId,
            OrganizationId organizationId,
            TenantId tenantId
    );
}
