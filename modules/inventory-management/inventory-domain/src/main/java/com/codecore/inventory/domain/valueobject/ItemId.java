package com.codecore.inventory.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Surrogate identifier for {@link com.codecore.inventory.domain.model.item.Item}.
 * Hard unique identity of the inventoriable catalog aggregate (ADR-016).
 */
public final class ItemId {

    private final UUID value;

    public ItemId(UUID value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public ItemId(String value) {
        this(UUID.fromString(Objects.requireNonNull(value, "value").trim()));
    }

    public static ItemId generate() {
        return new ItemId(UUID.randomUUID());
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
