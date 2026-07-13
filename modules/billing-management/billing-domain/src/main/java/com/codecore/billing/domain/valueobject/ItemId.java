package com.codecore.billing.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Reference to an Inventory {@code ItemId} on an optional {@link com.codecore.billing.domain.model.invoice.InvoiceLine}
 * (ADR-017). Item never gains price or billing fields — Billing is the sole cross-BC consumer here.
 */
public final class ItemId {

    private final UUID value;

    public ItemId(UUID value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public ItemId(String value) {
        this(UUID.fromString(Objects.requireNonNull(value, "value").trim()));
    }

    public static ItemId of(UUID itemId) {
        return new ItemId(itemId);
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
        ItemId that = (ItemId) other;
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
