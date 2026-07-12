package com.codecore.encounter.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Logical reference to Clinical Foundation PatientId (ADR-012 / ADR-015).
 * Validation via ReferencePorts in application — not this VO.
 */
public final class PatientId {

    private final UUID value;

    public PatientId(UUID value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public static PatientId of(UUID value) {
        return new PatientId(value);
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
