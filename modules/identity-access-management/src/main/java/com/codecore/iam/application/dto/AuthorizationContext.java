package com.codecore.iam.application.dto;

import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.MembershipId;
import com.codecore.iam.domain.valueobject.TenantId;

import java.util.Objects;

/**
 * Request-scoped authorization subject: active membership in the JWT tenant (ADR-007).
 */
public record AuthorizationContext(
        IdentityId identityId,
        TenantId tenantId,
        MembershipId membershipId
) {

    public AuthorizationContext {
        Objects.requireNonNull(identityId, "identityId");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(membershipId, "membershipId");
    }
}
