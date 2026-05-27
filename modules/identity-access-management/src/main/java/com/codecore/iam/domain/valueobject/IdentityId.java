package com.codecore.iam.domain.valueobject;

import java.util.UUID;

public final class IdentityId extends UuidIdentifier {

    public IdentityId(UUID value) {
        super(value);
    }

    public IdentityId(String value) {
        super(value);
    }

    public static IdentityId generate() {
        return new IdentityId(UUID.randomUUID());
    }
}
