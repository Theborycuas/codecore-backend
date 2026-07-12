package com.codecore.encounter.domain.model.encounter;

import com.codecore.encounter.domain.exception.InvalidEncounterStateException;
import com.codecore.encounter.domain.valueobject.AppointmentId;
import com.codecore.encounter.domain.valueobject.EncounterId;
import com.codecore.encounter.domain.valueobject.EncounterStatus;
import com.codecore.encounter.domain.valueobject.EncounterTimeBounds;
import com.codecore.encounter.domain.valueobject.OfficeId;
import com.codecore.encounter.domain.valueobject.OrganizationId;
import com.codecore.encounter.domain.valueobject.PatientId;
import com.codecore.encounter.domain.valueobject.StaffAssignmentId;
import com.codecore.encounter.domain.valueobject.TenantId;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Encounter aggregate root — care episode that occurred (or is recorded as having occurred)
 * for a care subject at a determined operational context (ADR-015).
 * <p>
 * Intentionally small: identity, time bounds, episode status, and ID references only.
 * Never embeds notes, SOAP, odontogram, labs, billing, or vertical rules.
 * Cross-BC ACTIVE/coherence checks belong in application via ReferencePorts (ADR-013).
 */
public final class Encounter {

    private final EncounterId id;
    private final TenantId tenantId;
    private PatientId patientId;
    private StaffAssignmentId staffAssignmentId;
    private OrganizationId organizationId;
    private OfficeId officeId;
    private AppointmentId appointmentId;
    private EncounterTimeBounds timeBounds;
    private EncounterStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private Encounter(
            EncounterId id,
            TenantId tenantId,
            PatientId patientId,
            StaffAssignmentId staffAssignmentId,
            OrganizationId organizationId,
            OfficeId officeId,
            AppointmentId appointmentId,
            EncounterTimeBounds timeBounds,
            EncounterStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.patientId = Objects.requireNonNull(patientId, "patientId");
        this.staffAssignmentId = Objects.requireNonNull(staffAssignmentId, "staffAssignmentId");
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId");
        this.officeId = officeId;
        this.appointmentId = appointmentId;
        this.timeBounds = Objects.requireNonNull(timeBounds, "timeBounds");
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    /**
     * Opens a new occurred episode in {@link EncounterStatus#IN_PROGRESS}.
     */
    public static Encounter open(
            EncounterId id,
            TenantId tenantId,
            PatientId patientId,
            StaffAssignmentId staffAssignmentId,
            OrganizationId organizationId,
            OfficeId officeId,
            AppointmentId appointmentId,
            Instant startedAt,
            Instant now
    ) {
        Objects.requireNonNull(now, "now");
        return new Encounter(
                id,
                tenantId,
                patientId,
                staffAssignmentId,
                organizationId,
                officeId,
                appointmentId,
                EncounterTimeBounds.open(startedAt),
                EncounterStatus.IN_PROGRESS,
                now,
                now
        );
    }

    public static Encounter reconstitute(
            EncounterId id,
            TenantId tenantId,
            PatientId patientId,
            StaffAssignmentId staffAssignmentId,
            OrganizationId organizationId,
            OfficeId officeId,
            AppointmentId appointmentId,
            EncounterTimeBounds timeBounds,
            EncounterStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Encounter(
                id,
                tenantId,
                patientId,
                staffAssignmentId,
                organizationId,
                officeId,
                appointmentId,
                timeBounds,
                status,
                createdAt,
                updatedAt
        );
    }

    public EncounterId id() {
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

    public Optional<AppointmentId> appointmentId() {
        return Optional.ofNullable(appointmentId);
    }

    public EncounterTimeBounds timeBounds() {
        return timeBounds;
    }

    public Instant startedAt() {
        return timeBounds.startedAt();
    }

    public Optional<Instant> endedAt() {
        return timeBounds.endedAt();
    }

    public EncounterStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public void changeStartedAt(Instant newStartedAt) {
        requireInProgress("change startedAt");
        this.timeBounds = timeBounds.withStartedAt(newStartedAt);
        touch();
    }

    public void assignEndedAt(Instant newEndedAt) {
        requireInProgress("assign endedAt");
        this.timeBounds = timeBounds.withEndedAt(newEndedAt);
        touch();
    }

    public void clearEndedAt() {
        requireInProgress("clear endedAt");
        this.timeBounds = timeBounds.withoutEndedAt();
        touch();
    }

    public void changePatient(PatientId newPatientId) {
        requireInProgress("change patient");
        this.patientId = Objects.requireNonNull(newPatientId, "newPatientId");
        touch();
    }

    public void changeStaffAssignment(StaffAssignmentId newStaffAssignmentId) {
        requireInProgress("change staff assignment");
        this.staffAssignmentId = Objects.requireNonNull(newStaffAssignmentId, "newStaffAssignmentId");
        touch();
    }

    public void changeOrganization(OrganizationId newOrganizationId) {
        requireInProgress("change organization");
        this.organizationId = Objects.requireNonNull(newOrganizationId, "newOrganizationId");
        touch();
    }

    public void assignOffice(OfficeId newOfficeId) {
        requireInProgress("assign office");
        this.officeId = Objects.requireNonNull(newOfficeId, "newOfficeId");
        touch();
    }

    public void clearOffice() {
        requireInProgress("clear office");
        this.officeId = null;
        touch();
    }

    public void linkAppointment(AppointmentId newAppointmentId) {
        requireInProgress("link appointment");
        this.appointmentId = Objects.requireNonNull(newAppointmentId, "newAppointmentId");
        touch();
    }

    public void clearAppointment() {
        requireInProgress("clear appointment");
        this.appointmentId = null;
        touch();
    }

    public void cancel() {
        requireInProgress("cancel");
        this.status = EncounterStatus.CANCELLED;
        touch();
    }

    /**
     * Completes the episode. {@code endedAt} is required and must be &gt;= {@code startedAt}.
     */
    public void complete(Instant endedAt) {
        requireInProgress("complete");
        this.timeBounds = timeBounds.withEndedAt(endedAt);
        this.status = EncounterStatus.COMPLETED;
        touch();
    }

    private void requireInProgress(String action) {
        if (status != EncounterStatus.IN_PROGRESS) {
            throw new InvalidEncounterStateException(
                    "Cannot " + action + " when encounter is " + status
            );
        }
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
