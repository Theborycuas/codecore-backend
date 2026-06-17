package com.codecore.iam.application.dto;

import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.IdentityStatus;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record AdminUserView(
        IdentityId id,
        String email,
        IdentityStatus status,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt
) {

    public AdminUserView {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
