package com.codecore.iam.domain.valueobject;

import com.codecore.iam.domain.exception.InvalidDomainValueException;

import java.util.Objects;

/**
 * Hashed credential representation. Must never contain plain text (blueprint: value-objects.md §6).
 */
public final class PasswordHash {

    private final String value;

    private PasswordHash(String value) {
        this.value = value;
    }

    public static PasswordHash ofHashedValue(String hashedValue) {
        Objects.requireNonNull(hashedValue, "hashedValue");
        String normalized = hashedValue.trim();
        if (normalized.isBlank()) {
            throw new InvalidDomainValueException("Password hash must not be blank");
        }
        return new PasswordHash(normalized);
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return "[protected]";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        PasswordHash that = (PasswordHash) other;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
