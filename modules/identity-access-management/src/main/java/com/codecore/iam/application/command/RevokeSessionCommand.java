package com.codecore.iam.application.command;

import com.codecore.iam.domain.valueobject.SessionId;
import com.codecore.iam.domain.valueobject.TenantId;

public record RevokeSessionCommand(
        TenantId tenantId,
        SessionId sessionId
) {
}
