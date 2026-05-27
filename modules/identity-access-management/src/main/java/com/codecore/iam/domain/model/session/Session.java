package com.codecore.iam.domain.model.session;

import com.codecore.iam.domain.exception.AuthenticationNotPermittedException;
import com.codecore.iam.domain.model.common.AggregateRoot;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.SessionId;
import com.codecore.iam.domain.valueobject.SessionStatus;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.domain.valueobject.TokenExpiration;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Session aggregate root — refresh token and session lifecycle.
 */
public final class Session extends AggregateRoot {

    private final SessionId id;
    private final IdentityId identityId;
    private SessionStatus status;
    private final ClientContext clientContext;
    private RefreshToken activeRefreshToken;
    private Instant lastActivityAt;
    private final TokenExpiration expiration;
    private Instant revokedAt;
    private final Instant createdAt;
    private Instant updatedAt;

    public Session(
            SessionId id,
            TenantId tenantId,
            IdentityId identityId,
            SessionStatus status,
            ClientContext clientContext,
            RefreshToken activeRefreshToken,
            Instant lastActivityAt,
            TokenExpiration expiration,
            Instant revokedAt,
            Instant createdAt,
            Instant updatedAt,
            long version
    ) {
        super(tenantId, version);
        this.id = Objects.requireNonNull(id, "id");
        this.identityId = Objects.requireNonNull(identityId, "identityId");
        this.status = Objects.requireNonNull(status, "status");
        this.clientContext = Objects.requireNonNullElse(clientContext, ClientContext.unknown());
        this.activeRefreshToken = activeRefreshToken;
        this.lastActivityAt = lastActivityAt;
        this.expiration = expiration;
        this.revokedAt = revokedAt;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public SessionId id() {
        return id;
    }

    public IdentityId identityId() {
        return identityId;
    }

    public SessionStatus status() {
        return status;
    }

    public ClientContext clientContext() {
        return clientContext;
    }

    public Optional<RefreshToken> activeRefreshToken() {
        return Optional.ofNullable(activeRefreshToken);
    }

    public Instant lastActivityAt() {
        return lastActivityAt;
    }

    public Optional<TokenExpiration> expiration() {
        return Optional.ofNullable(expiration);
    }

    public Optional<Instant> revokedAt() {
        return Optional.ofNullable(revokedAt);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public void validateRefreshEligibility(Instant now) {
        if (status != SessionStatus.ACTIVE) {
            throw new AuthenticationNotPermittedException("Session is not active");
        }
        if (revokedAt != null) {
            throw new AuthenticationNotPermittedException("Session is revoked");
        }
        if (expiration != null && expiration.isExpired(now)) {
            throw new AuthenticationNotPermittedException("Session is expired");
        }
        if (activeRefreshToken == null) {
            throw new AuthenticationNotPermittedException("Refresh token is missing");
        }
        activeRefreshToken.validateRefreshEligibility(now);
    }

    public void registerActivity(Instant at) {
        this.lastActivityAt = Objects.requireNonNull(at, "at");
        this.updatedAt = at;
        bumpVersion();
    }

    public void revoke(Instant at) {
        this.status = SessionStatus.REVOKED;
        this.revokedAt = Objects.requireNonNull(at, "at");
        if (activeRefreshToken != null) {
            activeRefreshToken.revoke(at);
        }
        this.updatedAt = at;
        bumpVersion();
    }

    public void rotateRefreshToken(RefreshToken rotatedToken, Instant at) {
        Objects.requireNonNull(rotatedToken, "rotatedToken");
        if (activeRefreshToken != null) {
            activeRefreshToken.revoke(at);
        }
        this.activeRefreshToken = rotatedToken;
        this.lastActivityAt = at;
        this.updatedAt = at;
        bumpVersion();
    }
}
