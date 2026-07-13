package com.codecore.audit.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Opaque resource identity referenced by an audit entry (ADR-020).
 * Not validated against foreign BC existence at append time.
 */
public final class ResourceId {

    private final UUID value;

    public ResourceId(UUID value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public ResourceId(String value) {
        this(UUID.fromString(Objects.requireNonNull(value, "value").trim()));
    }

    public static ResourceId of(UUID value) {
        return new ResourceId(value);
    }

    public static ResourceId generate() {
        return new ResourceId(UUID.randomUUID());
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
        ResourceId that = (ResourceId) other;
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
