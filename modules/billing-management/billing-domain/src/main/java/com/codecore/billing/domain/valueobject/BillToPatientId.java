package com.codecore.billing.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Reference to a Clinical Foundation {@code PatientId} used as the bill-to party (ADR-017).
 * Mutually exclusive with {@link BillToOrganizationId} — see {@link BillTo}.
 */
public final class BillToPatientId {

    private final UUID value;

    public BillToPatientId(UUID value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public BillToPatientId(String value) {
        this(UUID.fromString(Objects.requireNonNull(value, "value").trim()));
    }

    public static BillToPatientId of(UUID patientId) {
        return new BillToPatientId(patientId);
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
        BillToPatientId that = (BillToPatientId) other;
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
