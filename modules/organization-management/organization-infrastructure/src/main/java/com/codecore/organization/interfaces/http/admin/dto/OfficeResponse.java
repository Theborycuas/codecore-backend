package com.codecore.organization.interfaces.http.admin.dto;

import com.codecore.organization.application.dto.AdminOfficeView;

import java.time.Instant;
import java.util.UUID;

public record OfficeResponse(
        UUID id,
        UUID organizationId,
        String code,
        String name,
        String status,
        Instant createdAt,
        Instant updatedAt
) {

    public static OfficeResponse from(AdminOfficeView view) {
        return new OfficeResponse(
                view.id().value(),
                view.organizationId().value(),
                view.code(),
                view.name(),
                view.status().name(),
                view.createdAt(),
                view.updatedAt()
        );
    }
}
