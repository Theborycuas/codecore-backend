package com.codecore.patient.domain.valueobject;

import com.codecore.patient.domain.exception.InvalidDomainValueException;

import java.util.Objects;

/**
 * Optional typed external clinical identity key (document, MRN, chip, …) — ADR-012.
 * Soft-unique per tenant when enabled at persistence; not the hard identity ({@link PatientId}).
 */
public final class ExternalIdentifier {

    private static final int MAX_VALUE_LENGTH = 128;

    private final ExternalIdentifierType type;
    private final String value;

    private ExternalIdentifier(ExternalIdentifierType type, String value) {
        this.type = type;
        this.value = value;
    }

    public static ExternalIdentifier of(ExternalIdentifierType type, String rawValue) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(rawValue, "rawValue");
        String trimmed = rawValue.trim();
        if (trimmed.isBlank() || trimmed.length() > MAX_VALUE_LENGTH) {
            throw new InvalidDomainValueException("Invalid external identifier value");
        }
        return new ExternalIdentifier(type, trimmed);
    }

    public static ExternalIdentifier of(String type, String rawValue) {
        return of(ExternalIdentifierType.of(type), rawValue);
    }

    public ExternalIdentifierType type() {
        return type;
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
        ExternalIdentifier that = (ExternalIdentifier) other;
        return type.equals(that.type) && value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value);
    }

    @Override
    public String toString() {
        return type + "=" + value;
    }
}
