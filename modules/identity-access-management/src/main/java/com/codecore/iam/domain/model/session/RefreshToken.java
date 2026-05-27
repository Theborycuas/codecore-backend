package com.codecore.iam.domain.model.session;

import com.codecore.iam.domain.exception.AuthenticationNotPermittedException;
import com.codecore.iam.domain.valueobject.PasswordHash;
import com.codecore.iam.domain.valueobject.RefreshTokenId;
import com.codecore.iam.domain.valueobject.TokenExpiration;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Refresh token owned by the {@link Session} aggregate.
 */
public final class RefreshToken {

    private final RefreshTokenId id;
    private final PasswordHash tokenHash;
    private final Instant issuedAt;
    private final TokenExpiration expiration;
    private Instant revokedAt;
    private final Optional<RefreshTokenId> rotatedFrom;
    private long version;

    public RefreshToken(
            RefreshTokenId id,
            PasswordHash tokenHash,
            Instant issuedAt,
            TokenExpiration expiration,
            Instant revokedAt,
            Optional<RefreshTokenId> rotatedFrom,
            long version
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash");
        this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        this.expiration = Objects.requireNonNull(expiration, "expiration");
        this.revokedAt = revokedAt;
        this.rotatedFrom = Objects.requireNonNullElse(rotatedFrom, Optional.empty());
        this.version = version;
    }

    public RefreshTokenId id() {
        return id;
    }

    public PasswordHash tokenHash() {
        return tokenHash;
    }

    public Instant issuedAt() {
        return issuedAt;
    }

    public TokenExpiration expiration() {
        return expiration;
    }

    public Optional<Instant> revokedAt() {
        return Optional.ofNullable(revokedAt);
    }

    public Optional<RefreshTokenId> rotatedFrom() {
        return rotatedFrom;
    }

    public long version() {
        return version;
    }

    public void validateRefreshEligibility(Instant now) {
        if (revokedAt != null) {
            throw new AuthenticationNotPermittedException("Refresh token is revoked");
        }
        if (expiration.isExpired(now)) {
            throw new AuthenticationNotPermittedException("Refresh token is expired");
        }
    }

    public void revoke(Instant at) {
        this.revokedAt = Objects.requireNonNull(at, "at");
        this.version++;
    }
}
