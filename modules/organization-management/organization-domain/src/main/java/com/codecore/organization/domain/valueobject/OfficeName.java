package com.codecore.organization.domain.valueobject;

import com.codecore.organization.domain.exception.InvalidDomainValueException;

import java.util.Objects;

public final class OfficeName {

    private static final int MAX_LENGTH = 200;

    private final String value;

    private OfficeName(String value) {
        this.value = value;
    }

    public static OfficeName of(String raw) {
        Objects.requireNonNull(raw, "raw");
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            throw new InvalidDomainValueException("Office name is required");
        }
        if (trimmed.length() > MAX_LENGTH) {
            throw new InvalidDomainValueException("Office name exceeds maximum length");
        }
        return new OfficeName(trimmed);
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
        OfficeName that = (OfficeName) other;
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
