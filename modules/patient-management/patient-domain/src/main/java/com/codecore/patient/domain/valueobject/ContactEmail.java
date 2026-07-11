package com.codecore.patient.domain.valueobject;

import com.codecore.patient.domain.exception.InvalidDomainValueException;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Optional contact email — not a uniqueness key (ADR-012).
 */
public final class ContactEmail {

    private static final int MAX_LENGTH = 320;
    private static final Pattern FORMAT = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final String value;

    private ContactEmail(String value) {
        this.value = value;
    }

    public static ContactEmail of(String raw) {
        Objects.requireNonNull(raw, "raw");
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank() || normalized.length() > MAX_LENGTH || !FORMAT.matcher(normalized).matches()) {
            throw new InvalidDomainValueException("Invalid contact email");
        }
        return new ContactEmail(normalized);
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
        ContactEmail that = (ContactEmail) other;
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
