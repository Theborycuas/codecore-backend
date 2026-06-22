package com.codecore.organization.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Surrogate identifier for {@link com.codecore.organization.domain.model.organization.Organization}.
 */
public final class OrganizationId {

    private final UUID value;

    public OrganizationId(UUID value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public OrganizationId(String value) {
        this(UUID.fromString(Objects.requireNonNull(value, "value").trim()));
    }

    public static OrganizationId generate() {
        return new OrganizationId(UUID.randomUUID());
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
