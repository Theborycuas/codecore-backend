package com.codecore.iam.application.dto;

import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.domain.valueobject.TenantStatus;

import java.time.Instant;
import java.util.Objects;

public record AdminTenantView(
        TenantId id,
        String name,
        TenantStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public AdminTenantView {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
