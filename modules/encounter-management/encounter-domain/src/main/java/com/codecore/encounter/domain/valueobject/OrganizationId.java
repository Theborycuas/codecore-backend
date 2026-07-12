package com.codecore.encounter.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Logical reference to OrganizationId — denormalized episode context (ADR-015).
 * Validation via ReferencePorts in application — not this VO.
 */
public final class OrganizationId {

    private final UUID value;

    public OrganizationId(UUID value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public static OrganizationId of(UUID value) {
        return new OrganizationId(value);
    }

    public UUID value() {
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
