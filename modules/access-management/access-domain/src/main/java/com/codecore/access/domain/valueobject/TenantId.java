package com.codecore.access.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Reference to an IAM-owned tenant (ADR-003 isolation boundary).
 * Access does not own the tenant aggregate.
 * Immutable on {@link com.codecore.access.domain.model.invitation.Invitation} after create (ADR-019).
 */
public final class TenantId {

    private final UUID value;

    public TenantId(UUID value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public TenantId(String value) {
        this(UUID.fromString(Objects.requireNonNull(value, "value").trim()));
    }

    public static TenantId generate() {
        return new TenantId(UUID.randomUUID());
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
        TenantId that = (TenantId) other;
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
