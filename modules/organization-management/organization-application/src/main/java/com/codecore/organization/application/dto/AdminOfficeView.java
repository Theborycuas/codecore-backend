package com.codecore.organization.application.dto;

import com.codecore.organization.domain.valueobject.OfficeId;
import com.codecore.organization.domain.valueobject.OfficeStatus;
import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.organization.domain.valueobject.TenantId;

import java.time.Instant;

public record AdminOfficeView(
        OfficeId id,
        TenantId tenantId,
        OrganizationId organizationId,
        String code,
        String name,
        OfficeStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
