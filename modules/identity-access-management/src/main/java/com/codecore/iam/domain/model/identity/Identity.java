package com.codecore.iam.domain.model.identity;

import com.codecore.iam.domain.exception.AuthenticationNotPermittedException;
import com.codecore.iam.domain.model.common.AggregateRoot;
import com.codecore.iam.domain.model.loginattempt.LoginAttemptTracker;
import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.IdentityStatus;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.domain.valueobject.Username;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Identity aggregate root — authentication identity and credential ownership.
 */
public final class Identity extends AggregateRoot {

    private final IdentityId id;
    private final EmailAddress email;
    private final Username username;
    private IdentityStatus status;
    private Credential credential;
    private boolean locked;
    private boolean enabled;
    private Instant lastLoginAt;
    private final Instant createdAt;
    private Instant updatedAt;

    public Identity(
            IdentityId id,
            TenantId tenantId,
            EmailAddress email,
            Username username,
            IdentityStatus status,
            Credential credential,
            boolean locked,
            boolean enabled,
            Instant lastLoginAt,
            Instant createdAt,
            Instant updatedAt,
            long version
    ) {
        super(tenantId, version);
        this.id = Objects.requireNonNull(id, "id");
        this.email = Objects.requireNonNull(email, "email");
        this.username = Objects.requireNonNull(username, "username");
        this.status = Objects.requireNonNull(status, "status");
        this.credential = credential;
        this.locked = locked;
        this.enabled = enabled;
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

    public Username username() {
        return username;
    }

    public IdentityStatus status() {
        return status;
    }

    public Optional<Credential> credential() {
        return Optional.ofNullable(credential);
    }

    public boolean locked() {
        return locked;
    }

    public boolean enabled() {
        return enabled;
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
        if (!enabled) {
            throw new AuthenticationNotPermittedException("Identity is disabled");
        }
        if (locked || status.isLocked()) {
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
        this.locked = true;
        this.status = IdentityStatus.LOCKED;
        touch();
        bumpVersion();
    }

    public void unlockAccount() {
        this.locked = false;
        this.status = IdentityStatus.ACTIVE;
        touch();
        bumpVersion();
    }

    public void disable() {
        this.enabled = false;
        this.status = IdentityStatus.DISABLED;
        touch();
        bumpVersion();
    }

    public void enable() {
        this.enabled = true;
        this.status = IdentityStatus.ACTIVE;
        touch();
        bumpVersion();
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
