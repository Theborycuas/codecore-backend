package com.codecore.encounter.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Optional logical reference to Scheduling AppointmentId (ADR-014 / ADR-015).
 * Validation via ReferencePorts in application — not this VO.
 */
public final class AppointmentId {

    private final UUID value;

    public AppointmentId(UUID value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public static AppointmentId of(UUID value) {
        return new AppointmentId(value);
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
        AppointmentId that = (AppointmentId) other;
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
