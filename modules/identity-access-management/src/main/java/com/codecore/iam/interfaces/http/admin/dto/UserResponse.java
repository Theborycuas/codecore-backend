package com.codecore.iam.interfaces.http.admin.dto;

import com.codecore.iam.application.dto.AdminUserView;
import com.codecore.iam.application.dto.PagedResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String status,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static UserResponse from(AdminUserView view) {
        return new UserResponse(
                view.id().value(),
                view.email(),
                view.status().name(),
                view.lastLoginAt(),
                view.createdAt(),
                view.updatedAt()
        );
    }
}
