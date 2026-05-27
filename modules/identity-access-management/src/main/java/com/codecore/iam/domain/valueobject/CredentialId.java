package com.codecore.iam.domain.valueobject;

import java.util.UUID;

public final class CredentialId extends UuidIdentifier {

    public CredentialId(UUID value) {
        super(value);
    }

    public CredentialId(String value) {
        super(value);
    }

    public static CredentialId generate() {
        return new CredentialId(UUID.randomUUID());
    }
}
