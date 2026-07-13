package com.codecore.payment.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Local reference to the Billing {@code InvoiceId} being settled (ADR-018).
 * <p>
 * Payments does not own or embed the Invoice aggregate — this is a plain value object holding
 * the UUID; existence + ISSUED status is validated at write time via
 * {@code com.codecore.billing.contract.reference.InvoiceReferencePort} (ADR-013).
 */
public final class InvoiceId {

    private final UUID value;

    public InvoiceId(UUID value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public InvoiceId(String value) {
        this(UUID.fromString(Objects.requireNonNull(value, "value").trim()));
    }

    public static InvoiceId of(UUID value) {
        return new InvoiceId(value);
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
        InvoiceId that = (InvoiceId) other;
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
