package com.codecore.organization.application.dto;

import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.organization.domain.valueobject.OrganizationStatus;
import com.codecore.organization.domain.valueobject.TenantId;

import java.time.Instant;

public record AdminOrganizationView(
        OrganizationId id,
        TenantId tenantId,
        String code,
        String name,
        OrganizationStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
