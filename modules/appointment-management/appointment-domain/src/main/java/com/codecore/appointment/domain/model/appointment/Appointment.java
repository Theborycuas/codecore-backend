package com.codecore.appointment.domain.model.appointment;

import com.codecore.appointment.domain.exception.InvalidAppointmentStateException;
import com.codecore.appointment.domain.valueobject.AppointmentId;
import com.codecore.appointment.domain.valueobject.AppointmentStatus;
import com.codecore.appointment.domain.valueobject.AppointmentTimeWindow;
import com.codecore.appointment.domain.valueobject.OfficeId;
import com.codecore.appointment.domain.valueobject.OrganizationId;
import com.codecore.appointment.domain.valueobject.PatientId;
import com.codecore.appointment.domain.valueobject.StaffAssignmentId;
import com.codecore.appointment.domain.valueobject.TenantId;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Appointment aggregate root — planned commitment to provide a service to a care subject
 * at a determined time and operational context (ADR-014).
 * <p>
 * Intentionally small: identity, time window, commitment status, and ID references only.
 * Never embeds Encounter, clinical records, slots, recurrence, billing, or vertical rules.
 * Cross-BC ACTIVE/coherence checks belong in application via ReferencePorts (ADR-013).
 */
public final class Appointment {

    private final AppointmentId id;
    private final TenantId tenantId;
    private PatientId patientId;
    private StaffAssignmentId staffAssignmentId;
    private OrganizationId organizationId;
    private OfficeId officeId;
    private AppointmentTimeWindow timeWindow;
    private AppointmentStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private Appointment(
            AppointmentId id,
            TenantId tenantId,
            PatientId patientId,
            StaffAssignmentId staffAssignmentId,
            OrganizationId organizationId,
            OfficeId officeId,
            AppointmentTimeWindow timeWindow,
            AppointmentStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.patientId = Objects.requireNonNull(patientId, "patientId");
        this.staffAssignmentId = Objects.requireNonNull(staffAssignmentId, "staffAssignmentId");
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId");
        this.officeId = officeId;
        this.timeWindow = Objects.requireNonNull(timeWindow, "timeWindow");
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    /**
     * Creates a new planned commitment in {@link AppointmentStatus#SCHEDULED}.
     */
    public static Appointment schedule(
            AppointmentId id,
            TenantId tenantId,
            PatientId patientId,
            StaffAssignmentId staffAssignmentId,
            OrganizationId organizationId,
            OfficeId officeId,
            AppointmentTimeWindow timeWindow,
            Instant now
    ) {
        Objects.requireNonNull(now, "now");
        return new Appointment(
                id,
                tenantId,
                patientId,
                staffAssignmentId,
                organizationId,
                officeId,
                timeWindow,
                AppointmentStatus.SCHEDULED,
                now,
                now
        );
    }

    public static Appointment reconstitute(
            AppointmentId id,
            TenantId tenantId,
            PatientId patientId,
            StaffAssignmentId staffAssignmentId,
            OrganizationId organizationId,
            OfficeId officeId,
            AppointmentTimeWindow timeWindow,
            AppointmentStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Appointment(
                id,
                tenantId,
                patientId,
                staffAssignmentId,
                organizationId,
                officeId,
                timeWindow,
                status,
                createdAt,
                updatedAt
        );
    }

    public AppointmentId id() {
        return id;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public PatientId patientId() {
        return patientId;
    }

    public StaffAssignmentId staffAssignmentId() {
        return staffAssignmentId;
    }

    public OrganizationId organizationId() {
        return organizationId;
    }

    public Optional<OfficeId> officeId() {
        return Optional.ofNullable(officeId);
    }

    public AppointmentTimeWindow timeWindow() {
        return timeWindow;
    }

    public Instant startsAt() {
        return timeWindow.startsAt();
    }

    public Instant endsAt() {
        return timeWindow.endsAt();
    }

    public AppointmentStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public void reschedule(AppointmentTimeWindow newTimeWindow) {
        requireScheduled("reschedule");
        this.timeWindow = Objects.requireNonNull(newTimeWindow, "newTimeWindow");
        touch();
    }

    public void changePatient(PatientId newPatientId) {
        requireScheduled("change patient");
        this.patientId = Objects.requireNonNull(newPatientId, "newPatientId");
        touch();
    }

    public void changeStaffAssignment(StaffAssignmentId newStaffAssignmentId) {
        requireScheduled("change staff assignment");
        this.staffAssignmentId = Objects.requireNonNull(newStaffAssignmentId, "newStaffAssignmentId");
        touch();
    }

    public void changeOrganization(OrganizationId newOrganizationId) {
        requireScheduled("change organization");
        this.organizationId = Objects.requireNonNull(newOrganizationId, "newOrganizationId");
        touch();
    }

    public void assignOffice(OfficeId newOfficeId) {
        requireScheduled("assign office");
        this.officeId = Objects.requireNonNull(newOfficeId, "newOfficeId");
        touch();
    }

    public void clearOffice() {
        requireScheduled("clear office");
        this.officeId = null;
        touch();
    }

    public void cancel() {
        requireScheduled("cancel");
        this.status = AppointmentStatus.CANCELLED;
        touch();
    }

    public void complete() {
        requireScheduled("complete");
        this.status = AppointmentStatus.COMPLETED;
        touch();
    }

    private void requireScheduled(String action) {
        if (status != AppointmentStatus.SCHEDULED) {
            throw new InvalidAppointmentStateException(
                    "Cannot " + action + " when appointment is " + status
            );
        }
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
