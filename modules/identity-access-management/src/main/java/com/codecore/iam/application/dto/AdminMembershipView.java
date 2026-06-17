package com.codecore.iam.application.dto;

import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.MembershipId;
import com.codecore.iam.domain.valueobject.MembershipStatus;
import com.codecore.iam.domain.valueobject.TenantId;

import java.time.Instant;
import java.util.Objects;

public record AdminMembershipView(
        MembershipId id,
        IdentityId identityId,
        TenantId tenantId,
        MembershipStatus status,
        String identityEmail,
        Instant createdAt,
        Instant updatedAt
) {

    public AdminMembershipView {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(identityId, "identityId");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
