package com.codecore.billing.domain.valueobject;

import com.codecore.billing.domain.exception.InvalidDomainValueException;

import java.util.Objects;

/**
 * Optional human-facing invoice number (ADR-017).
 * <p>
 * Soft-uniqueness per tenant {@code (tenantId, invoiceNumber)} is enforced by application +
 * persistence, not by this value object.
 */
public final class InvoiceNumber {

    private static final int MAX_LENGTH = 64;

    private final String value;

    private InvoiceNumber(String value) {
        this.value = value;
    }

    public static InvoiceNumber of(String raw) {
        Objects.requireNonNull(raw, "raw");
        String trimmed = raw.trim();
        if (trimmed.isBlank() || trimmed.length() > MAX_LENGTH) {
            throw new InvalidDomainValueException("Invalid invoice number");
        }
        return new InvoiceNumber(trimmed);
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
        InvoiceNumber that = (InvoiceNumber) other;
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
