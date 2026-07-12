package com.codecore.encounter.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Surrogate identifier for {@link com.codecore.encounter.domain.model.encounter.Encounter}.
 * Hard unique identity of the occurred care episode aggregate (ADR-015).
 */
public final class EncounterId {

    private final UUID value;

    public EncounterId(UUID value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public EncounterId(String value) {
        this(UUID.fromString(Objects.requireNonNull(value, "value").trim()));
    }

    public static EncounterId generate() {
        return new EncounterId(UUID.randomUUID());
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
        EncounterId that = (EncounterId) other;
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
