package com.codecore.iam.application.dto;

import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.domain.valueobject.TenantName;
import com.codecore.iam.domain.valueobject.TenantStatus;

/**
 * Successful tenant creation result (application layer).
 */
public record CreateTenantResponse(
        TenantId tenantId,
        TenantName name,
        TenantStatus status
) {
}
