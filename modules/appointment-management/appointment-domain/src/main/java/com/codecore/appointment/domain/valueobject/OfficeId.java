package com.codecore.appointment.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Logical reference to Organization Management {@code OfficeId} (ADR-010 / ADR-014).
 * Optional location of the planned commitment.
 */
public final class OfficeId {

    private final UUID value;

    public OfficeId(UUID value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public static OfficeId of(UUID value) {
        return new OfficeId(value);
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
        OfficeId that = (OfficeId) other;
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
