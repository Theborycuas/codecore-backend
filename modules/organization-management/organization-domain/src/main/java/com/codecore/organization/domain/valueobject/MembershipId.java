package com.codecore.organization.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Reference to an IAM-owned membership (ADR-006). Organization Management does not own the aggregate.
 */
public final class MembershipId {

    private final UUID value;

    public MembershipId(UUID value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public UUID value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        MembershipId that = (MembershipId) other;
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
