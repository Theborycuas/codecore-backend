package com.codecore.iam.interfaces.http.admin.dto;

import com.codecore.iam.application.dto.AdminTenantView;

import java.time.Instant;
import java.util.UUID;

public record TenantResponse(
        UUID tenantId,
        String name,
        String status,
        Instant createdAt,
        Instant updatedAt
) {

    public static TenantResponse from(AdminTenantView view) {
        return new TenantResponse(
                view.id().value(),
                view.name(),
                view.status().name(),
                view.createdAt(),
                view.updatedAt()
        );
    }
}
