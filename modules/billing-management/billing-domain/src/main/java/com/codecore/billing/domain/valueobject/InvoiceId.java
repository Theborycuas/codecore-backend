package com.codecore.billing.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Surrogate identifier for {@link com.codecore.billing.domain.model.invoice.Invoice}.
 * Hard unique identity of the commercial claim aggregate (ADR-017).
 */
public final class InvoiceId {

    private final UUID value;

    public InvoiceId(UUID value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public InvoiceId(String value) {
        this(UUID.fromString(Objects.requireNonNull(value, "value").trim()));
    }

    public static InvoiceId generate() {
        return new InvoiceId(UUID.randomUUID());
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
