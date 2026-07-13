package com.codecore.billing.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Surrogate identifier for {@link com.codecore.billing.domain.model.invoice.InvoiceLine}.
 * Internal entity identity — {@link InvoiceLine} is never addressed outside its owning Invoice.
 */
public final class InvoiceLineId {

    private final UUID value;

    public InvoiceLineId(UUID value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public InvoiceLineId(String value) {
        this(UUID.fromString(Objects.requireNonNull(value, "value").trim()));
    }

    public static InvoiceLineId generate() {
        return new InvoiceLineId(UUID.randomUUID());
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
        InvoiceLineId that = (InvoiceLineId) other;
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
