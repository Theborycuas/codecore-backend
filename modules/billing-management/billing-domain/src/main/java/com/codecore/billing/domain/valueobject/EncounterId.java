package com.codecore.billing.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Reference to a Clinical Records {@code EncounterId} on an optional {@link com.codecore.billing.domain.model.invoice.InvoiceLine}
 * (ADR-017). When the Invoice bill-to is a Patient, the encounter's patientId must match it.
 */
public final class EncounterId {

    private final UUID value;

    public EncounterId(UUID value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public EncounterId(String value) {
        this(UUID.fromString(Objects.requireNonNull(value, "value").trim()));
    }

    public static EncounterId of(UUID encounterId) {
        return new EncounterId(encounterId);
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
        EncounterId that = (EncounterId) other;
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
