package com.codecore.payment.domain.valueobject;

import com.codecore.payment.domain.exception.InvalidDomainValueException;

import java.util.Objects;

/**
 * Optional opaque payment method label (ADR-018) — e.g. {@code CASH}, {@code CARD},
 * {@code WIRE_TRANSFER}, or a PSP-defined code. Payments does not interpret, validate against a
 * fixed enum, or route based on this value — it is a passthrough label capped at 32 chars.
 */
public final class PaymentMethodCode {

    private static final int MAX_LENGTH = 32;

    private final String value;

    private PaymentMethodCode(String value) {
        this.value = value;
    }

    public static PaymentMethodCode of(String raw) {
        Objects.requireNonNull(raw, "raw");
        String trimmed = raw.trim();
        if (trimmed.isBlank() || trimmed.length() > MAX_LENGTH) {
            throw new InvalidDomainValueException("Invalid payment method code");
        }
        return new PaymentMethodCode(trimmed);
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
        PaymentMethodCode that = (PaymentMethodCode) other;
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
