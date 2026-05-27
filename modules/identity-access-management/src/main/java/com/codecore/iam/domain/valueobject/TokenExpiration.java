package com.codecore.iam.domain.valueobject;

import java.time.Instant;
import java.util.Objects;

public final class TokenExpiration {

    private final Instant expiresAt;

    private TokenExpiration(Instant expiresAt) {
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
    }

    public static TokenExpiration at(Instant expiresAt) {
        return new TokenExpiration(expiresAt);
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(Objects.requireNonNull(now, "now"));
    }

    public Instant expiresAt() {
        return expiresAt;
    }
}
