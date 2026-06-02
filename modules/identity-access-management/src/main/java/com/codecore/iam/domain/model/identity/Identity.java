package com.codecore.iam.domain.model.identity;

import com.codecore.iam.domain.exception.AuthenticationNotPermittedException;
import com.codecore.iam.domain.model.common.AggregateRoot;
import com.codecore.iam.domain.model.loginattempt.LoginAttemptTracker;
import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.IdentityStatus;
import com.codecore.iam.domain.valueobject.TenantId;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Identity aggregate root — tenant-scoped authentication identity (email-first).
 * Lifecycle is governed solely by {@link IdentityStatus}.
 */
public final class Identity extends AggregateRoot {

    private final IdentityId id;
    private final EmailAddress email;
    private IdentityStatus status;
    private Credential credential;
    private Instant lastLoginAt;
    private final Instant createdAt;
    private Instant updatedAt;

    public Identity(
            IdentityId id,
            TenantId tenantId,
            EmailAddress email,
            IdentityStatus status,
            Credential credential,
            Instant lastLoginAt,
            Instant createdAt,
            Instant updatedAt,
            long version
    ) {
        super(tenantId, version);
        this.id = Objects.requireNonNull(id, "id");
        this.email = Objects.requireNonNull(email, "email");
        this.status = Objects.requireNonNull(status, "status");
        this.credential = credential;
        this.lastLoginAt = lastLoginAt;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public IdentityId id() {
        return id;
    }

    public EmailAddress email() {
        return email;
    }

    public IdentityStatus status() {
        return status;
    }

    public Optional<Credential> credential() {
        return Optional.ofNullable(credential);
    }

    /**
     * Convenience view of email verification derived from {@link #status()} only.
     * <p>
     * <strong>Source of truth:</strong> {@link IdentityStatus}, not {@code iam.iam_user.email_verified}.
     * The database column is a persisted projection for SQL/reporting; never use
     * {@code entity.getEmailVerifiedProjection()} (or similar) to drive domain rules.
     */
    public boolean isEmailVerified() {
        return status != IdentityStatus.PENDING_VERIFICATION;
    }

    public Instant lastLoginAt() {
        return lastLoginAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public void ensureMayAuthenticate(LoginAttemptTracker attemptTracker, Instant now) {
        Objects.requireNonNull(attemptTracker, "attemptTracker");
        Objects.requireNonNull(now, "now");
        if (attemptTracker.isTemporarilyLocked(now)) {
            throw new AuthenticationNotPermittedException("Identity is temporarily locked");
        }
        validateAuthenticationEligibility();
    }

    public void validateAuthenticationEligibility() {
        if (status == IdentityStatus.DISABLED) {
            throw new AuthenticationNotPermittedException("Identity is disabled");
        }
        if (status == IdentityStatus.LOCKED) {
            throw new AuthenticationNotPermittedException("Identity is locked");
        }
        if (status == IdentityStatus.PENDING_VERIFICATION) {
            throw new AuthenticationNotPermittedException("Identity is pending verification");
        }
        if (status == IdentityStatus.PASSWORD_RESET_REQUIRED) {
            throw new AuthenticationNotPermittedException("Password reset is required");
        }
        if (!status.mayAuthenticate()) {
            throw new AuthenticationNotPermittedException("Identity may not authenticate");
        }
        if (credential == null) {
            throw new AuthenticationNotPermittedException("Credential is missing");
        }
    }

    public void markLastLogin(Instant at) {
        this.lastLoginAt = Objects.requireNonNull(at, "at");
        touch();
        bumpVersion();
    }

    public void lockAccount() {
        this.status = IdentityStatus.LOCKED;
        touch();
        bumpVersion();
    }

    public void unlockAccount() {
        this.status = IdentityStatus.ACTIVE;
        touch();
        bumpVersion();
    }

    public void disable() {
        this.status = IdentityStatus.DISABLED;
        touch();
        bumpVersion();
    }

    public void enable() {
        this.status = IdentityStatus.ACTIVE;
        touch();
        bumpVersion();
    }

    public void markEmailVerified() {
        if (this.status == IdentityStatus.PENDING_VERIFICATION) {
            this.status = IdentityStatus.ACTIVE;
            touch();
            bumpVersion();
        }
    }

    public void requirePasswordReset() {
        this.status = IdentityStatus.PASSWORD_RESET_REQUIRED;
        if (credential != null) {
            credential.requirePasswordReset();
        }
        touch();
        bumpVersion();
    }

    public void attachCredential(Credential newCredential) {
        this.credential = Objects.requireNonNull(newCredential, "newCredential");
        touch();
        bumpVersion();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
