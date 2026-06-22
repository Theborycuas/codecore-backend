package com.codecore.iam.domain.valueobject;

import com.codecore.iam.domain.exception.InvalidDomainValueException;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Globally unique permission identifier in {@code resource:action} form (e.g. {@code user:create}).
 */
public final class PermissionCode {

    private static final int MAX_LENGTH = 150;
    private static final Pattern FORMAT = Pattern.compile("^[a-z][a-z0-9_-]*:[a-z][a-z0-9_-]*$");

    private final String value;

    private PermissionCode(String value) {
        this.value = value;
    }

    public static PermissionCode of(String raw) {
        Objects.requireNonNull(raw, "raw");
        String normalized = raw.trim().toLowerCase();
        if (normalized.isBlank() || normalized.length() > MAX_LENGTH || !FORMAT.matcher(normalized).matches()) {
            throw new InvalidDomainValueException("Invalid permission code");
        }
        return new PermissionCode(normalized);
    }

    public String resource() {
        return value.substring(0, value.indexOf(':'));
    }

    public String action() {
        return value.substring(value.indexOf(':') + 1);
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
        PermissionCode that = (PermissionCode) other;
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
