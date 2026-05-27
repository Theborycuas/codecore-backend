package com.codecore.iam.application.command;

import com.codecore.iam.domain.valueobject.SessionId;
import com.codecore.iam.domain.valueobject.TenantId;

public record LogoutCommand(
        TenantId tenantId,
        SessionId sessionId
) {
}
