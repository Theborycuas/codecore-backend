package com.codecore.iam.domain.valueobject;

import java.util.UUID;

public final class FailedLoginAttemptId extends UuidIdentifier {

    public FailedLoginAttemptId(UUID value) {
        super(value);
    }

    public FailedLoginAttemptId(String value) {
        super(value);
    }

    public static FailedLoginAttemptId generate() {
        return new FailedLoginAttemptId(UUID.randomUUID());
    }
}
