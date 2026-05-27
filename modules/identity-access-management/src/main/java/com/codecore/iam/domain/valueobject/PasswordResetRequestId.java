package com.codecore.iam.domain.valueobject;

import java.util.UUID;

public final class PasswordResetRequestId extends UuidIdentifier {

    public PasswordResetRequestId(UUID value) {
        super(value);
    }

    public PasswordResetRequestId(String value) {
        super(value);
    }

    public static PasswordResetRequestId generate() {
        return new PasswordResetRequestId(UUID.randomUUID());
    }
}
