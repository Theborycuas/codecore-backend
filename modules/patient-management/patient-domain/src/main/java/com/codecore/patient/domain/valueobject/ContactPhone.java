package com.codecore.patient.domain.valueobject;

import com.codecore.patient.domain.exception.InvalidDomainValueException;

import java.util.Objects;

/**
 * Optional contact phone — not a uniqueness key (ADR-012). Format is intentionally permissive for multi-country SaaS.
 */
public final class ContactPhone {

    private static final int MAX_LENGTH = 32;

    private final String value;

    private ContactPhone(String value) {
        this.value = value;
    }

    public static ContactPhone of(String raw) {
        Objects.requireNonNull(raw, "raw");
        String trimmed = raw.trim();
        if (trimmed.isBlank() || trimmed.length() > MAX_LENGTH) {
            throw new InvalidDomainValueException("Invalid contact phone");
        }
        return new ContactPhone(trimmed);
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
        ContactPhone that = (ContactPhone) other;
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
