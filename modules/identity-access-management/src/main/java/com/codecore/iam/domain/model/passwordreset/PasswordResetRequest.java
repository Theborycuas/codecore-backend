package com.codecore.iam.domain.model.passwordreset;

import com.codecore.iam.domain.exception.AuthenticationNotPermittedException;
import com.codecore.iam.domain.model.common.AggregateRoot;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.PasswordResetRequestId;
import com.codecore.iam.domain.valueobject.PasswordResetStatus;
import com.codecore.iam.domain.valueobject.ResetTokenHash;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.domain.valueobject.TokenExpiration;

import java.time.Instant;
import java.util.Objects;

/**
 * Password reset aggregate root.
 */
public final class PasswordResetRequest extends AggregateRoot {

    private final PasswordResetRequestId id;
    private final IdentityId identityId;
    private final ResetTokenHash resetTokenHash;
    private final TokenExpiration expiration;
    private PasswordResetStatus status;
    private Instant usedAt;
    private final Instant createdAt;
    private Instant updatedAt;

    public PasswordResetRequest(
            PasswordResetRequestId id,
            TenantId tenantId,
            IdentityId identityId,
            ResetTokenHash resetTokenHash,
            TokenExpiration expiration,
            PasswordResetStatus status,
            Instant usedAt,
            Instant createdAt,
            Instant updatedAt,
            long version
    ) {
        super(tenantId, version);
        this.id = Objects.requireNonNull(id, "id");
        this.identityId = Objects.requireNonNull(identityId, "identityId");
        this.resetTokenHash = Objects.requireNonNull(resetTokenHash, "resetTokenHash");
        this.expiration = Objects.requireNonNull(expiration, "expiration");
        this.status = Objects.requireNonNull(status, "status");
        this.usedAt = usedAt;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    /**
     * Creates a pending password reset request (token hash only — never store the raw token).
     */
    public static PasswordResetRequest create(
            TenantId tenantId,
            IdentityId identityId,
            ResetTokenHash resetTokenHash,
            TokenExpiration expiration,
            Instant now
    ) {
        Objects.requireNonNull(now, "now");
        return new PasswordResetRequest(
                PasswordResetRequestId.generate(),
                tenantId,
                identityId,
                resetTokenHash,
                expiration,
                PasswordResetStatus.PENDING,
                null,
                now,
                now,
                0L
        );
    }

    public PasswordResetRequestId id() {
        return id;
    }

    public IdentityId identityId() {
        return identityId;
    }

    public ResetTokenHash resetTokenHash() {
        return resetTokenHash;
    }

    public TokenExpiration expiration() {
        return expiration;
    }

    public PasswordResetStatus status() {
        return status;
    }

    public Instant usedAt() {
        return usedAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public void validateResetEligibility(Instant now) {
        if (!status.mayCompleteReset()) {
            throw new AuthenticationNotPermittedException("Password reset is not pending");
        }
        if (expiration.isExpired(now)) {
            throw new AuthenticationNotPermittedException("Password reset token is expired");
        }
    }

    public void markUsed(Instant at) {
        validateResetEligibility(at);
        this.status = PasswordResetStatus.USED;
        this.usedAt = Objects.requireNonNull(at, "at");
        this.updatedAt = at;
        bumpVersion();
    }

    public void expire(Instant at) {
        this.status = PasswordResetStatus.EXPIRED;
        this.updatedAt = Objects.requireNonNull(at, "at");
        bumpVersion();
    }

    public void cancel(Instant at) {
        this.status = PasswordResetStatus.CANCELLED;
        this.updatedAt = Objects.requireNonNull(at, "at");
        bumpVersion();
    }
}
