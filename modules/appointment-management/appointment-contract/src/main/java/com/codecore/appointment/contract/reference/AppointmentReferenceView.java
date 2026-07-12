package com.codecore.appointment.contract.reference;

import com.codecore.appointment.domain.valueobject.AppointmentId;
import com.codecore.appointment.domain.valueobject.AppointmentStatus;
import com.codecore.appointment.domain.valueobject.PatientId;

import java.util.Objects;

/**
 * Minimal immutable reference of an Appointment for consumer write-time invariants (ADR-013 / ADR-015).
 * Not an aggregate, entity, or admin DTO.
 * <p>
 * Only returned for linkable statuses ({@code SCHEDULED}, {@code COMPLETED}).
 */
public final class AppointmentReferenceView {

    private final AppointmentId appointmentId;
    private final PatientId patientId;
    private final AppointmentStatus status;

    public AppointmentReferenceView(
            AppointmentId appointmentId,
            PatientId patientId,
            AppointmentStatus status
    ) {
        this.appointmentId = Objects.requireNonNull(appointmentId, "appointmentId");
        this.patientId = Objects.requireNonNull(patientId, "patientId");
        this.status = Objects.requireNonNull(status, "status");
        if (status != AppointmentStatus.SCHEDULED && status != AppointmentStatus.COMPLETED) {
            throw new IllegalArgumentException(
                    "AppointmentReferenceView only allows SCHEDULED or COMPLETED, got " + status
            );
        }
    }

    public AppointmentId appointmentId() {
        return appointmentId;
    }

    public PatientId patientId() {
        return patientId;
    }

    public AppointmentStatus status() {
        return status;
    }

    public boolean isLinkableForEncounter() {
        return status == AppointmentStatus.SCHEDULED || status == AppointmentStatus.COMPLETED;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        AppointmentReferenceView that = (AppointmentReferenceView) other;
        return appointmentId.equals(that.appointmentId)
                && patientId.equals(that.patientId)
                && status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(appointmentId, patientId, status);
    }
}
