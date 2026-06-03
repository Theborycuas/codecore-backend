package com.codecore.iam.application.dto;

import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.IdentityStatus;
import com.codecore.iam.domain.valueobject.TenantId;

import java.util.Optional;

/**
 * Authenticated identity material extracted from a validated access token.
 */
public record AuthenticatedPrincipal(
        IdentityId identityId,
        String email,
        IdentityStatus status,
        Optional<TenantId> tenantId
) {
}
