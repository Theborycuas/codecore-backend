package com.codecore.iam.domain.valueobject;

import java.util.UUID;

public final class LoginAttemptTrackerId extends UuidIdentifier {

    public LoginAttemptTrackerId(UUID value) {
        super(value);
    }

    public LoginAttemptTrackerId(String value) {
        super(value);
    }

    public static LoginAttemptTrackerId generate() {
        return new LoginAttemptTrackerId(UUID.randomUUID());
    }
}
