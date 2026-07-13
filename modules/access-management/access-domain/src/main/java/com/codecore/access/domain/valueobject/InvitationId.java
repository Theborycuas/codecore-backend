package com.codecore.access.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Surrogate identifier for {@link com.codecore.access.domain.model.invitation.Invitation}.
 * Hard unique identity of the invitation aggregate (ADR-019).
 */
public final class InvitationId {

    private final UUID value;

    public InvitationId(UUID value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public InvitationId(String value) {
        this(UUID.fromString(Objects.requireNonNull(value, "value").trim()));
    }

    public static InvitationId generate() {
        return new InvitationId(UUID.randomUUID());
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
        InvitationId that = (InvitationId) other;
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
