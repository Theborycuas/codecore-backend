package com.codecore.access.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Local reference to an IAM {@code MembershipId} (inviter or resulting membership) (ADR-019).
 * <p>
 * Access does not own or embed the Membership aggregate — this is a plain value object holding
 * the UUID; existence and ACTIVE status are validated at write time via IAM ports.
 */
public final class MembershipId {

    private final UUID value;

    public MembershipId(UUID value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public MembershipId(String value) {
        this(UUID.fromString(Objects.requireNonNull(value, "value").trim()));
    }

    public static MembershipId of(UUID value) {
        return new MembershipId(value);
    }

    public static MembershipId generate() {
        return new MembershipId(UUID.randomUUID());
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
