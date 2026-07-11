package com.codecore.patient.domain.valueobject;

import com.codecore.patient.domain.exception.InvalidDomainValueException;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Typed external identifier category (e.g. {@code NATIONAL_ID}, {@code MRN}, {@code MICROCHIP}).
 * Free of country-specific mandates (ADR-012).
 */
public final class ExternalIdentifierType {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 64;
    private static final Pattern FORMAT = Pattern.compile("^[A-Z][A-Z0-9_]*$");

    private final String value;

    private ExternalIdentifierType(String value) {
        this.value = value;
    }

    public static ExternalIdentifierType of(String raw) {
        Objects.requireNonNull(raw, "raw");
        String normalized = raw.trim()
                .toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
        if (normalized.length() < MIN_LENGTH || normalized.length() > MAX_LENGTH) {
            throw new InvalidDomainValueException("Invalid external identifier type length");
        }
        if (!FORMAT.matcher(normalized).matches()) {
            throw new InvalidDomainValueException("Invalid external identifier type format");
        }
        return new ExternalIdentifierType(normalized);
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
        ExternalIdentifierType that = (ExternalIdentifierType) other;
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
