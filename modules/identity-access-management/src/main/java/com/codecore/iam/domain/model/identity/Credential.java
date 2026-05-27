package com.codecore.iam.domain.model.identity;

import com.codecore.iam.domain.valueobject.CredentialId;
import com.codecore.iam.domain.valueobject.PasswordHash;

import java.time.Instant;
import java.util.Objects;

/**
 * Credential owned by the {@link Identity} aggregate.
 */
public final class Credential {

    private final CredentialId id;
    private PasswordHash passwordHash;
    private Instant passwordChangedAt;
    private Instant passwordExpiresAt;
    private boolean mustChangePassword;
    private long version;

    public Credential(
            CredentialId id,
            PasswordHash passwordHash,
            Instant passwordChangedAt,
            Instant passwordExpiresAt,
            boolean mustChangePassword,
            long version
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.passwordChangedAt = passwordChangedAt;
        this.passwordExpiresAt = passwordExpiresAt;
        this.mustChangePassword = mustChangePassword;
        this.version = version;
    }

    public CredentialId id() {
        return id;
    }

    public PasswordHash passwordHash() {
        return passwordHash;
    }

    public Instant passwordChangedAt() {
        return passwordChangedAt;
    }

    public Instant passwordExpiresAt() {
        return passwordExpiresAt;
    }

    public boolean mustChangePassword() {
        return mustChangePassword;
    }

    public long version() {
        return version;
    }

    public void rotatePassword(PasswordHash newHash, Instant changedAt) {
        this.passwordHash = Objects.requireNonNull(newHash, "newHash");
        this.passwordChangedAt = Objects.requireNonNull(changedAt, "changedAt");
        this.mustChangePassword = false;
        this.version++;
    }

    public void requirePasswordReset() {
        this.mustChangePassword = true;
        this.version++;
    }
}
