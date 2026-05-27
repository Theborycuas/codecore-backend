package com.codecore.iam.domain.model.loginattempt;

import com.codecore.iam.domain.model.common.AggregateRoot;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.LoginAttemptTrackerId;
import com.codecore.iam.domain.valueobject.TenantId;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Login attempt aggregate root — brute-force counters and temporary lockout.
 */
public final class LoginAttemptTracker extends AggregateRoot {

    private final LoginAttemptTrackerId id;
    private final IdentityId identityId;
    private int failedAttempts;
    private Instant lastFailedAt;
    private Instant lockedUntil;
    private final Instant createdAt;
    private Instant updatedAt;

    public LoginAttemptTracker(
            LoginAttemptTrackerId id,
            TenantId tenantId,
            IdentityId identityId,
            int failedAttempts,
            Instant lastFailedAt,
            Instant lockedUntil,
            Instant createdAt,
            Instant updatedAt,
            long version
    ) {
        super(tenantId, version);
        this.id = Objects.requireNonNull(id, "id");
        this.identityId = Objects.requireNonNull(identityId, "identityId");
        this.failedAttempts = failedAttempts;
        this.lastFailedAt = lastFailedAt;
        this.lockedUntil = lockedUntil;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public LoginAttemptTrackerId id() {
        return id;
    }

    public IdentityId identityId() {
        return identityId;
    }

    public int failedAttempts() {
        return failedAttempts;
    }

    public Optional<Instant> lastFailedAt() {
        return Optional.ofNullable(lastFailedAt);
    }

    public Optional<Instant> lockedUntil() {
        return Optional.ofNullable(lockedUntil);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public void registerFailure(Instant now, int maxAttempts, Duration lockDuration) {
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(lockDuration, "lockDuration");
        failedAttempts++;
        lastFailedAt = now;
        if (failedAttempts >= maxAttempts) {
            lockedUntil = now.plus(lockDuration);
        }
        updatedAt = now;
        bumpVersion();
    }

    public void registerSuccess(Instant now) {
        Objects.requireNonNull(now, "now");
        failedAttempts = 0;
        lockedUntil = null;
        lastFailedAt = null;
        updatedAt = now;
        bumpVersion();
    }

    public boolean isTemporarilyLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }
}
