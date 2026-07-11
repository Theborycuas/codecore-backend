package com.codecore.patient.domain.model.patient;

import com.codecore.patient.domain.exception.InvalidPatientStateException;
import com.codecore.patient.domain.valueobject.ExternalIdentifiers;
import com.codecore.patient.domain.valueobject.PatientDemographics;
import com.codecore.patient.domain.valueobject.PatientId;
import com.codecore.patient.domain.valueobject.PatientStatus;
import com.codecore.patient.domain.valueobject.PrimaryOrganizationId;
import com.codecore.patient.domain.valueobject.TenantId;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Patient aggregate root — clinical registry identity of a care subject (ADR-012).
 * <p>
 * <strong>One sentence:</strong> the clinical registry identity of the care subject.
 * <p>
 * Intentionally small: demographics, optional external identifiers, optional primary
 * organization reference, and registry lifecycle. Never embeds appointments, encounters,
 * records, billing, offices, staff, or IAM identity.
 */
public final class Patient {

    private final PatientId id;
    private final TenantId tenantId;
    private PatientDemographics demographics;
    private ExternalIdentifiers externalIdentifiers;
    private PrimaryOrganizationId primaryOrganizationId;
    private PatientStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private Patient(
            PatientId id,
            TenantId tenantId,
            PatientDemographics demographics,
            ExternalIdentifiers externalIdentifiers,
            PrimaryOrganizationId primaryOrganizationId,
            PatientStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.demographics = Objects.requireNonNull(demographics, "demographics");
        this.externalIdentifiers = Objects.requireNonNull(externalIdentifiers, "externalIdentifiers");
        this.primaryOrganizationId = primaryOrganizationId;
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static Patient create(
            PatientId id,
            TenantId tenantId,
            PatientDemographics demographics,
            Instant now
    ) {
        return create(id, tenantId, demographics, ExternalIdentifiers.empty(), null, now);
    }

    public static Patient create(
            PatientId id,
            TenantId tenantId,
            PatientDemographics demographics,
            ExternalIdentifiers externalIdentifiers,
            PrimaryOrganizationId primaryOrganizationId,
            Instant now
    ) {
        Objects.requireNonNull(now, "now");
        return new Patient(
                id,
                tenantId,
                demographics,
                externalIdentifiers == null ? ExternalIdentifiers.empty() : externalIdentifiers,
                primaryOrganizationId,
                PatientStatus.ACTIVE,
                now,
                now
        );
    }

    public static Patient reconstitute(
            PatientId id,
            TenantId tenantId,
            PatientDemographics demographics,
            ExternalIdentifiers externalIdentifiers,
            PrimaryOrganizationId primaryOrganizationId,
            PatientStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Patient(
                id,
                tenantId,
                demographics,
                externalIdentifiers,
                primaryOrganizationId,
                status,
                createdAt,
                updatedAt
        );
    }

    public PatientId id() {
        return id;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public PatientDemographics demographics() {
        return demographics;
    }

    public ExternalIdentifiers externalIdentifiers() {
        return externalIdentifiers;
    }

    public Optional<PrimaryOrganizationId> primaryOrganizationId() {
        return Optional.ofNullable(primaryOrganizationId);
    }

    public PatientStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public void updateDemographics(PatientDemographics newDemographics) {
        requireActive("update demographics");
        this.demographics = Objects.requireNonNull(newDemographics, "newDemographics");
        touch();
    }

    public void replaceExternalIdentifiers(ExternalIdentifiers identifiers) {
        requireActive("replace external identifiers");
        this.externalIdentifiers = Objects.requireNonNull(identifiers, "identifiers");
        touch();
    }

    public void assignPrimaryOrganization(PrimaryOrganizationId organizationId) {
        requireActive("assign primary organization");
        this.primaryOrganizationId = Objects.requireNonNull(organizationId, "organizationId");
        touch();
    }

    public void removePrimaryOrganization() {
        requireActive("remove primary organization");
        this.primaryOrganizationId = null;
        touch();
    }

    public void archive() {
        if (status == PatientStatus.ARCHIVED) {
            throw new InvalidPatientStateException("Patient is already archived");
        }
        this.status = PatientStatus.ARCHIVED;
        touch();
    }

    public void activate() {
        if (status == PatientStatus.ACTIVE) {
            throw new InvalidPatientStateException("Patient is already active");
        }
        this.status = PatientStatus.ACTIVE;
        touch();
    }

    private void requireActive(String action) {
        if (status != PatientStatus.ACTIVE) {
            throw new InvalidPatientStateException("Cannot " + action + " when patient is archived");
        }
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
