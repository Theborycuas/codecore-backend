package com.codecore.inventory.interfaces.http.admin.dto;

import com.codecore.inventory.application.dto.AdminItemView;

import java.time.Instant;
import java.util.UUID;

public record ItemResponse(
        UUID id,
        String displayName,
        String code,
        UUID primaryOrganizationId,
        String status,
        Instant createdAt,
        Instant updatedAt
) {

    public static ItemResponse from(AdminItemView view) {
        return new ItemResponse(
                view.id().value(),
                view.displayName(),
                view.code(),
                view.primaryOrganizationUuid(),
                view.status().name(),
                view.createdAt(),
                view.updatedAt()
        );
    }
}
