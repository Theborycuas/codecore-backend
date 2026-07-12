package com.codecore.appointment.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Surrogate identifier for {@link com.codecore.appointment.domain.model.appointment.Appointment}.
 * Hard unique identity of the planned commitment aggregate (ADR-014).
 */
public final class AppointmentId {

    private final UUID value;

    public AppointmentId(UUID value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public AppointmentId(String value) {
        this(UUID.fromString(Objects.requireNonNull(value, "value").trim()));
    }

    public static AppointmentId generate() {
        return new AppointmentId(UUID.randomUUID());
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
