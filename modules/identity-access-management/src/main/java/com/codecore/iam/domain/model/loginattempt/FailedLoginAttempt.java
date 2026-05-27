package com.codecore.iam.domain.model.loginattempt;

import com.codecore.iam.domain.valueobject.FailedLoginAttemptId;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.TenantId;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable audit record for a failed authentication (persisted separately from tracker counters).
 */
public record FailedLoginAttempt(
        FailedLoginAttemptId id,
        TenantId tenantId,
        IdentityId identityId,
        String clientIp,
        String userAgent,
        String failureReason,
        Instant attemptedAt
) {

    public FailedLoginAttempt {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(identityId, "identityId");
        Objects.requireNonNull(failureReason, "failureReason");
        Objects.requireNonNull(attemptedAt, "attemptedAt");
        clientIp = Objects.requireNonNullElse(clientIp, "unknown");
        userAgent = Objects.requireNonNullElse(userAgent, "unknown");
    }
}
