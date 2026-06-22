package com.codecore.organization.domain.valueobject;

import com.codecore.organization.domain.exception.InvalidDomainValueException;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Functional business identifier for an organization within a tenant.
 * Normalized to {@code UPPER_SNAKE_CASE} (e.g. {@code DENTAL_NORTE}, {@code CARDIOLOGIA}).
 */
public final class OrganizationCode {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 64;
    private static final Pattern FORMAT = Pattern.compile("^[A-Z][A-Z0-9_]*$");

    private final String value;

    private OrganizationCode(String value) {
        this.value = value;
    }

    public static OrganizationCode of(String raw) {
        Objects.requireNonNull(raw, "raw");
        String normalized = normalize(raw);
        if (normalized.length() < MIN_LENGTH || normalized.length() > MAX_LENGTH) {
            throw new InvalidDomainValueException("Invalid organization code length");
        }
        if (!FORMAT.matcher(normalized).matches()) {
            throw new InvalidDomainValueException("Invalid organization code format");
        }
        return new OrganizationCode(normalized);
    }

    public String value() {
        return value;
    }

    private static String normalize(String raw) {
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            throw new InvalidDomainValueException("Organization code is required");
        }
        return trimmed
                .toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        OrganizationCode that = (OrganizationCode) other;
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
