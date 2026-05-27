package com.codecore.iam.domain.valueobject;

import java.util.UUID;

public final class RefreshTokenId extends UuidIdentifier {

    public RefreshTokenId(UUID value) {
        super(value);
    }

    public RefreshTokenId(String value) {
        super(value);
    }

    public static RefreshTokenId generate() {
        return new RefreshTokenId(UUID.randomUUID());
    }
}
