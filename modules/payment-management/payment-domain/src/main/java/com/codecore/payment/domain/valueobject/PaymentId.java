package com.codecore.payment.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Surrogate identifier for {@link com.codecore.payment.domain.model.payment.Payment}.
 * Hard unique identity of the payment aggregate (ADR-018).
 */
public final class PaymentId {

    private final UUID value;

    public PaymentId(UUID value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public PaymentId(String value) {
        this(UUID.fromString(Objects.requireNonNull(value, "value").trim()));
    }

    public static PaymentId generate() {
        return new PaymentId(UUID.randomUUID());
    }

    public UUID value() {
        return value;
    }

    public String asString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        PaymentId that = (PaymentId) other;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
