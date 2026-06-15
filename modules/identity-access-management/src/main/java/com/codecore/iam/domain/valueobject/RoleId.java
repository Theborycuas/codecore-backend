package com.codecore.iam.domain.valueobject;

import java.util.UUID;

/**
 * Surrogate identifier for the {@code Role} aggregate.
 */
public final class RoleId extends UuidIdentifier {

    public RoleId(UUID value) {
        super(value);
    }

    public RoleId(String value) {
        super(value);
    }

    public static RoleId generate() {
        return new RoleId(UUID.randomUUID());
    }
}
