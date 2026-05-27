package com.codecore.iam.domain.valueobject;

import java.util.UUID;

public final class SessionId extends UuidIdentifier {

    public SessionId(UUID value) {
        super(value);
    }

    public SessionId(String value) {
        super(value);
    }

    public static SessionId generate() {
        return new SessionId(UUID.randomUUID());
    }
}
