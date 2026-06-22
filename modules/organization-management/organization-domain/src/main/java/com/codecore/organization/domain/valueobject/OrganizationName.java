package com.codecore.organization.domain.valueobject;

import com.codecore.organization.domain.exception.InvalidDomainValueException;

import java.util.Objects;

/**
 * Human-readable organization name.
 */
public final class OrganizationName {

    private static final int MAX_LENGTH = 200;

    private final String value;

    private OrganizationName(String value) {
        this.value = value;
    }

    public static OrganizationName of(String raw) {
        Objects.requireNonNull(raw, "raw");
        String trimmed = raw.trim();
        if (trimmed.isBlank() || trimmed.length() > MAX_LENGTH) {
            throw new InvalidDomainValueException("Invalid organization name");
        }
        return new OrganizationName(trimmed);
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
        OrganizationName that = (OrganizationName) other;
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
