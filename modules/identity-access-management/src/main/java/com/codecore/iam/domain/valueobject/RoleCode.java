package com.codecore.iam.domain.valueobject;

import com.codecore.iam.domain.exception.InvalidDomainValueException;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stable machine identifier for a tenant-scoped role (e.g. {@code ADMIN}, {@code READ_ONLY}).
 */
public final class RoleCode {

    private static final int MAX_LENGTH = 100;
    private static final Pattern FORMAT = Pattern.compile("^[A-Z][A-Z0-9_]{0,99}$");

    private final String value;

    private RoleCode(String value) {
        this.value = value;
    }

    public static RoleCode of(String raw) {
        Objects.requireNonNull(raw, "raw");
        String normalized = raw.trim().toUpperCase();
        if (normalized.isBlank() || normalized.length() > MAX_LENGTH || !FORMAT.matcher(normalized).matches()) {
            throw new InvalidDomainValueException("Invalid role code");
        }
        return new RoleCode(normalized);
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
        RoleCode roleCode = (RoleCode) other;
        return value.equals(roleCode.value);
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
