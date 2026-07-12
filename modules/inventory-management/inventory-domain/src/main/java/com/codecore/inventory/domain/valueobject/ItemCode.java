package com.codecore.inventory.domain.valueobject;

import com.codecore.inventory.domain.exception.InvalidDomainValueException;

import java.util.Objects;

/**
 * Optional human SKU / material code for an Item (ADR-016).
 * <p>
 * Soft-uniqueness per tenant {@code (tenantId, code)} is enforced by application + persistence,
 * not by this value object.
 */
public final class ItemCode {

    private static final int MAX_LENGTH = 64;

    private final String value;

    private ItemCode(String value) {
        this.value = value;
    }

    public static ItemCode of(String raw) {
        Objects.requireNonNull(raw, "raw");
        String trimmed = raw.trim();
        if (trimmed.isBlank() || trimmed.length() > MAX_LENGTH) {
            throw new InvalidDomainValueException("Invalid item code");
        }
        return new ItemCode(trimmed);
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
        ItemCode that = (ItemCode) other;
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
