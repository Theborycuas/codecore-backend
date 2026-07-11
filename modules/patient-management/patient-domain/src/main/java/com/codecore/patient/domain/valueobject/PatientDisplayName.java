package com.codecore.patient.domain.valueobject;

import com.codecore.patient.domain.exception.InvalidDomainValueException;

import java.util.Objects;

/**
 * Registry display name of the care subject (human or animal) — intentionally generic (ADR-012).
 */
public final class PatientDisplayName {

    private static final int MAX_LENGTH = 200;

    private final String value;

    private PatientDisplayName(String value) {
        this.value = value;
    }

    public static PatientDisplayName of(String raw) {
        Objects.requireNonNull(raw, "raw");
        String trimmed = raw.trim();
        if (trimmed.isBlank() || trimmed.length() > MAX_LENGTH) {
            throw new InvalidDomainValueException("Invalid patient display name");
        }
        return new PatientDisplayName(trimmed);
    }

    public String value() {
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
        PatientDisplayName that = (PatientDisplayName) other;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
