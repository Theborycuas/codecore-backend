package com.codecore.iam.domain.valueobject;

import com.codecore.iam.domain.exception.InvalidDomainValueException;

import java.util.Objects;

/**
 * Human-readable tenant name.
 */
public final class TenantName {

    private static final int MAX_LENGTH = 200;

    private final String value;

    private TenantName(String value) {
        this.value = value;
    }

    public static TenantName of(String raw) {
        Objects.requireNonNull(raw, "raw");
        String trimmed = raw.trim();
        if (trimmed.isBlank() || trimmed.length() > MAX_LENGTH) {
            throw new InvalidDomainValueException("Invalid tenant name");
        }
        return new TenantName(trimmed);
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
        TenantName that = (TenantName) other;
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
