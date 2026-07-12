package com.codecore.inventory.domain.valueobject;

import com.codecore.inventory.domain.exception.InvalidDomainValueException;

import java.util.Objects;

/**
 * Operational display label of an inventoriable item (ADR-016).
 */
public final class ItemDisplayName {

    private static final int MAX_LENGTH = 200;

    private final String value;

    private ItemDisplayName(String value) {
        this.value = value;
    }

    public static ItemDisplayName of(String raw) {
        Objects.requireNonNull(raw, "raw");
        String trimmed = raw.trim();
        if (trimmed.isBlank() || trimmed.length() > MAX_LENGTH) {
            throw new InvalidDomainValueException("Invalid item display name");
        }
        return new ItemDisplayName(trimmed);
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
        ItemDisplayName that = (ItemDisplayName) other;
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
