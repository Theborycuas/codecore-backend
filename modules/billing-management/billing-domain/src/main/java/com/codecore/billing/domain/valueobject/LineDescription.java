package com.codecore.billing.domain.valueobject;

import com.codecore.billing.domain.exception.InvalidDomainValueException;

import java.util.Objects;

/**
 * Non-blank human description of an {@link com.codecore.billing.domain.model.invoice.InvoiceLine} (ADR-017).
 * Intentionally free text — no quantity / unit-of-measure semantics.
 */
public final class LineDescription {

    private static final int MAX_LENGTH = 500;

    private final String value;

    private LineDescription(String value) {
        this.value = value;
    }

    public static LineDescription of(String raw) {
        Objects.requireNonNull(raw, "raw");
        String trimmed = raw.trim();
        if (trimmed.isBlank() || trimmed.length() > MAX_LENGTH) {
            throw new InvalidDomainValueException("Invalid line description");
        }
        return new LineDescription(trimmed);
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
        LineDescription that = (LineDescription) other;
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
