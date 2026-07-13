package com.codecore.access.domain.valueobject;

import com.codecore.access.domain.exception.InvalidDomainValueException;

import java.util.Locale;
import java.util.Objects;

/**
 * Normalized invitee email address (ADR-019). Lowercased and trimmed; must contain {@code @}.
 */
public final class EmailAddress {

    private final String value;

    private EmailAddress(String value) {
        this.value = value;
    }

    public static EmailAddress of(String raw) {
        Objects.requireNonNull(raw, "raw");
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank() || !normalized.contains("@")) {
            throw new InvalidDomainValueException("Invalid email address");
        }
        return new EmailAddress(normalized);
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
        EmailAddress that = (EmailAddress) other;
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
