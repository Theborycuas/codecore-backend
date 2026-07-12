package com.codecore.inventory.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Optional administrative grouping / custodial organization for an Item (ADR-016).
 * <p>
 * Wraps an Organization Management {@code OrganizationId} as a UUID reference.
 * Does <strong>not</strong> mean ownership of stock.
 * ACTIVE + same-tenant validation on write is enforced by application ports (ADR-013), not by loading Organization here.
 */
public final class PrimaryOrganizationId {

    private final UUID value;

    public PrimaryOrganizationId(UUID value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public PrimaryOrganizationId(String value) {
        this(UUID.fromString(Objects.requireNonNull(value, "value").trim()));
    }

    public static PrimaryOrganizationId of(UUID organizationId) {
        return new PrimaryOrganizationId(organizationId);
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
        PrimaryOrganizationId that = (PrimaryOrganizationId) other;
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
