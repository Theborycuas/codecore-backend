package com.codecore.iam.application.command;

import com.codecore.iam.domain.valueobject.TenantId;

public record RefreshAccessTokenCommand(
        TenantId tenantId,
        String presentedRefreshToken
) {
}
