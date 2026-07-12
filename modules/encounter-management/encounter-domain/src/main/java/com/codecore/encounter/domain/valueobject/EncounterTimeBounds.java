package com.codecore.encounter.domain.valueobject;

import com.codecore.encounter.domain.exception.InvalidDomainValueException;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Time bounds of an Encounter (UTC). {@code startedAt} is always present;
 * {@code endedAt} is optional while {@code IN_PROGRESS} and required on complete.
 * When present, {@code endedAt} must be greater than or equal to {@code startedAt} (ADR-015).
 */
public final class EncounterTimeBounds {

    private final Instant startedAt;
    private final Instant endedAt;

    private EncounterTimeBounds(Instant startedAt, Instant endedAt) {
        this.startedAt = startedAt;
        this.endedAt = endedAt;
    }

    public static EncounterTimeBounds open(Instant startedAt) {
        Objects.requireNonNull(startedAt, "startedAt");
        return new EncounterTimeBounds(startedAt, null);
    }

    public static EncounterTimeBounds of(Instant startedAt, Instant endedAt) {
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(endedAt, "endedAt");
        requireEndedAtNotBeforeStartedAt(startedAt, endedAt);
        return new EncounterTimeBounds(startedAt, endedAt);
    }

    public EncounterTimeBounds withStartedAt(Instant newStartedAt) {
        Objects.requireNonNull(newStartedAt, "newStartedAt");
        if (endedAt != null) {
            requireEndedAtNotBeforeStartedAt(newStartedAt, endedAt);
        }
        return new EncounterTimeBounds(newStartedAt, endedAt);
    }

    public EncounterTimeBounds withEndedAt(Instant newEndedAt) {
        Objects.requireNonNull(newEndedAt, "newEndedAt");
        requireEndedAtNotBeforeStartedAt(startedAt, newEndedAt);
        return new EncounterTimeBounds(startedAt, newEndedAt);
    }

    public EncounterTimeBounds withoutEndedAt() {
        return new EncounterTimeBounds(startedAt, null);
    }

    private static void requireEndedAtNotBeforeStartedAt(Instant startedAt, Instant endedAt) {
        if (endedAt.isBefore(startedAt)) {
            throw new InvalidDomainValueException("endedAt must be greater than or equal to startedAt");
        }
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Optional<Instant> endedAt() {
        return Optional.ofNullable(endedAt);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        EncounterTimeBounds that = (EncounterTimeBounds) other;
        return startedAt.equals(that.startedAt) && Objects.equals(endedAt, that.endedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startedAt, endedAt);
    }
}
