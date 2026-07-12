package com.codecore.inventory.application.dto;

import com.codecore.inventory.domain.valueobject.ItemId;
import com.codecore.inventory.domain.valueobject.ItemStatus;
import com.codecore.inventory.domain.valueobject.PrimaryOrganizationId;
import com.codecore.inventory.domain.valueobject.TenantId;

import java.time.Instant;
import java.util.UUID;

public record AdminItemView(
        ItemId id,
        TenantId tenantId,
        String displayName,
        String code,
        PrimaryOrganizationId primaryOrganizationId,
        ItemStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public UUID primaryOrganizationUuid() {
        return primaryOrganizationId == null ? null : primaryOrganizationId.value();
    }
}
