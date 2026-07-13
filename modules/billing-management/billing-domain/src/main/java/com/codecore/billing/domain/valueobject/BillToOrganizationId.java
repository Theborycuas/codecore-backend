package com.codecore.billing.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Reference to an Organization Management {@code OrganizationId} used as a B2B bill-to party
 * (ADR-017). Must differ from the issuer {@link OrganizationId}.
 * Mutually exclusive with {@link BillToPatientId} — see {@link BillTo}.
 */
public final class BillToOrganizationId {

    private final UUID value;

    public BillToOrganizationId(UUID value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public BillToOrganizationId(String value) {
        this(UUID.fromString(Objects.requireNonNull(value, "value").trim()));
    }

    public static BillToOrganizationId of(UUID organizationId) {
        return new BillToOrganizationId(organizationId);
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
        BillToOrganizationId that = (BillToOrganizationId) other;
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
