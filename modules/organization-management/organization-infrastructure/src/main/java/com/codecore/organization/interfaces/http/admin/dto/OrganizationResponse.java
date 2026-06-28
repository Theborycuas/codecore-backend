package com.codecore.organization.interfaces.http.admin.dto;

import com.codecore.organization.application.dto.AdminOrganizationView;

import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String code,
        String name,
        String status,
        Instant createdAt,
        Instant updatedAt
) {

    public static OrganizationResponse from(AdminOrganizationView view) {
        return new OrganizationResponse(
                view.id().value(),
                view.code(),
                view.name(),
                view.status().name(),
                view.createdAt(),
                view.updatedAt()
        );
    }
}
