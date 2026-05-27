package com.codecore.iam.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Shared UUID-backed identifier semantics for IAM value objects.
 */
public abstract class UuidIdentifier {

    private final UUID value;

    protected UuidIdentifier(UUID value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    protected UuidIdentifier(String value) {
        this(UUID.fromString(Objects.requireNonNull(value, "value").trim()));
    }

    public UUID value() {
        return value;
    }

    public String asString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        UuidIdentifier that = (UuidIdentifier) other;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
