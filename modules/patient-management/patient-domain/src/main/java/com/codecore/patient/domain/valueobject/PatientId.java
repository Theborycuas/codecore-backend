package com.codecore.patient.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Surrogate identifier for {@link com.codecore.patient.domain.model.patient.Patient}.
 * Hard unique identity of the clinical registry aggregate (ADR-012).
 */
public final class PatientId {

    private final UUID value;

    public PatientId(UUID value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public PatientId(String value) {
        this(UUID.fromString(Objects.requireNonNull(value, "value").trim()));
    }

    public static PatientId generate() {
        return new PatientId(UUID.randomUUID());
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
        PatientId that = (PatientId) other;
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
