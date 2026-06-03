package com.codecore.iam.application.dto;

import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.IdentityStatus;
import com.codecore.iam.domain.valueobject.TenantId;

/**
 * Successful authentication outcome — no tokens, no credential material.
 */
public record AuthenticationResult(
        IdentityId identityId,
        TenantId tenantId,
        EmailAddress email,
        IdentityStatus status
) {
}
