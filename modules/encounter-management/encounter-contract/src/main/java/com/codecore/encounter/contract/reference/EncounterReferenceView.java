package com.codecore.encounter.contract.reference;

import com.codecore.encounter.domain.valueobject.EncounterId;
import com.codecore.encounter.domain.valueobject.EncounterStatus;
import com.codecore.encounter.domain.valueobject.PatientId;

import java.util.Objects;

/**
 * Minimal immutable reference of an Encounter for consumer write-time invariants (ADR-013 / ADR-015).
 * Not an aggregate, entity, or admin DTO.
 * <p>
 * Only returned for linkable statuses ({@code IN_PROGRESS}, {@code COMPLETED}).
 */
public final class EncounterReferenceView {

    private final EncounterId encounterId;
    private final PatientId patientId;
    private final EncounterStatus status;

    public EncounterReferenceView(
            EncounterId encounterId,
            PatientId patientId,
            EncounterStatus status
    ) {
        this.encounterId = Objects.requireNonNull(encounterId, "encounterId");
        this.patientId = Objects.requireNonNull(patientId, "patientId");
        this.status = Objects.requireNonNull(status, "status");
        if (status != EncounterStatus.IN_PROGRESS && status != EncounterStatus.COMPLETED) {
            throw new IllegalArgumentException(
                    "EncounterReferenceView only allows IN_PROGRESS or COMPLETED, got " + status
            );
        }
    }

    public EncounterId encounterId() {
        return encounterId;
    }

    public PatientId patientId() {
        return patientId;
    }

    public EncounterStatus status() {
        return status;
    }

    public boolean isLinkableForClinicalDocs() {
        return status == EncounterStatus.IN_PROGRESS || status == EncounterStatus.COMPLETED;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        EncounterReferenceView that = (EncounterReferenceView) other;
        return encounterId.equals(that.encounterId)
                && patientId.equals(that.patientId)
                && status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(encounterId, patientId, status);
    }
}
