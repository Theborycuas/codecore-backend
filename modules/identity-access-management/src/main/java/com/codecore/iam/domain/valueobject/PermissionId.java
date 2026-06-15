package com.codecore.iam.domain.valueobject;

import java.util.UUID;

/**
 * Surrogate identifier for the {@code Permission} aggregate.
 */
public final class PermissionId extends UuidIdentifier {

    public PermissionId(UUID value) {
        super(value);
    }

    public PermissionId(String value) {
        super(value);
    }

    public static PermissionId generate() {
        return new PermissionId(UUID.randomUUID());
    }
}
