package com.codecore.iam.application.dto;

import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.IdentityStatus;
import com.codecore.iam.domain.valueobject.TenantId;

/**
 * Outcome of a successful identity registration.
 */
public record RegisterIdentityResult(
        IdentityId identityId,
        TenantId tenantId,
        EmailAddress email,
        IdentityStatus status
) {
}
