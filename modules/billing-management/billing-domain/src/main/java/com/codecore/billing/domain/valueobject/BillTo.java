package com.codecore.billing.domain.valueobject;

import com.codecore.billing.domain.exception.InvalidDomainValueException;

import java.util.Objects;
import java.util.Optional;

/**
 * The debtor party of an Invoice — exactly one of {@link BillToPatientId} (clinical / self-pay)
 * or {@link BillToOrganizationId} (B2B) (ADR-017 §7). No guest / anonymous / Customer aggregate.
 */
public final class BillTo {

    private final BillToPatientId patientId;
    private final BillToOrganizationId organizationId;

    private BillTo(BillToPatientId patientId, BillToOrganizationId organizationId) {
        this.patientId = patientId;
        this.organizationId = organizationId;
    }

    public static BillTo patient(BillToPatientId patientId) {
        Objects.requireNonNull(patientId, "patientId");
        return new BillTo(patientId, null);
    }

    public static BillTo organization(BillToOrganizationId organizationId) {
        Objects.requireNonNull(organizationId, "organizationId");
        return new BillTo(null, organizationId);
    }

    /**
     * Reconstitutes a BillTo from persisted, already-validated nullable columns.
     * Exactly one argument must be non-null — enforced here for defense in depth.
     */
    public static BillTo of(BillToPatientId patientId, BillToOrganizationId organizationId) {
        boolean hasPatient = patientId != null;
        boolean hasOrganization = organizationId != null;
        if (hasPatient == hasOrganization) {
            throw new InvalidDomainValueException(
                    "Exactly one of billToPatientId or billToOrganizationId is required");
        }
        return new BillTo(patientId, organizationId);
    }

    public boolean isPatient() {
        return patientId != null;
    }

    public boolean isOrganization() {
        return organizationId != null;
    }

    public Optional<BillToPatientId> patientId() {
        return Optional.ofNullable(patientId);
    }

    public Optional<BillToOrganizationId> organizationId() {
        return Optional.ofNullable(organizationId);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        BillTo that = (BillTo) other;
        return Objects.equals(patientId, that.patientId) && Objects.equals(organizationId, that.organizationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(patientId, organizationId);
    }

    @Override
    public String toString() {
        return isPatient() ? "BillTo{patientId=" + patientId + "}" : "BillTo{organizationId=" + organizationId + "}";
    }
}
