package com.codecore.encounter.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Logical reference to Organization StaffAssignmentId (ADR-011 / ADR-015).
 * Validation via ReferencePorts in application — not this VO.
 */
public final class StaffAssignmentId {

    private final UUID value;

    public StaffAssignmentId(UUID value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public static StaffAssignmentId of(UUID value) {
        return new StaffAssignmentId(value);
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
        StaffAssignmentId that = (StaffAssignmentId) other;
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
