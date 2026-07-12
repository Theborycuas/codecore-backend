package com.codecore.appointment.domain.valueobject;

import com.codecore.appointment.domain.exception.InvalidDomainValueException;

import java.time.Instant;
import java.util.Objects;

/**
 * Planned time window of an Appointment (UTC instants). {@code endsAt} must be strictly after {@code startsAt}.
 */
public final class AppointmentTimeWindow {

    private final Instant startsAt;
    private final Instant endsAt;

    private AppointmentTimeWindow(Instant startsAt, Instant endsAt) {
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    public static AppointmentTimeWindow of(Instant startsAt, Instant endsAt) {
        Objects.requireNonNull(startsAt, "startsAt");
        Objects.requireNonNull(endsAt, "endsAt");
        if (!endsAt.isAfter(startsAt)) {
            throw new InvalidDomainValueException("endsAt must be strictly after startsAt");
        }
        return new AppointmentTimeWindow(startsAt, endsAt);
    }

    public Instant startsAt() {
        return startsAt;
    }

    public Instant endsAt() {
        return endsAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        AppointmentTimeWindow that = (AppointmentTimeWindow) other;
        return startsAt.equals(that.startsAt) && endsAt.equals(that.endsAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startsAt, endsAt);
    }
}
