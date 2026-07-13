package com.codecore.billing.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Reference to the issuing Organization Management {@code OrganizationId} (ADR-017).
 * Always required on {@link com.codecore.billing.domain.model.invoice.Invoice} — the emitting party.
 * ACTIVE + same-tenant validation on write is enforced by application ports (ADR-013), not here.
 */
public final class OrganizationId {

    private final UUID value;

    public OrganizationId(UUID value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public OrganizationId(String value) {
        this(UUID.fromString(Objects.requireNonNull(value, "value").trim()));
    }

    public static OrganizationId of(UUID organizationId) {
        return new OrganizationId(organizationId);
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
        OrganizationId that = (OrganizationId) other;
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
