package com.codecore.patient.infrastructure.persistence.mapper;

import com.codecore.patient.domain.model.patient.Patient;
import com.codecore.patient.domain.valueobject.ContactEmail;
import com.codecore.patient.domain.valueobject.ContactPhone;
import com.codecore.patient.domain.valueobject.DateOfBirth;
import com.codecore.patient.domain.valueobject.ExternalIdentifiers;
import com.codecore.patient.domain.valueobject.PatientDemographics;
import com.codecore.patient.domain.valueobject.PatientDisplayName;
import com.codecore.patient.domain.valueobject.PatientId;
import com.codecore.patient.domain.valueobject.PatientStatus;
import com.codecore.patient.domain.valueobject.PrimaryOrganizationId;
import com.codecore.patient.domain.valueobject.TenantId;
import com.codecore.patient.infrastructure.persistence.entity.PatientEntity;

/**
 * Isomorphic mapping between {@link PatientEntity} and {@link Patient}.
 */
public final class PatientMapper {

    public Patient toDomain(PatientEntity entity, ExternalIdentifiers externalIdentifiers) {
        PatientDemographics demographics = PatientDemographics.of(
                PatientDisplayName.of(entity.getDisplayName()),
                entity.getContactEmail() == null ? null : ContactEmail.of(entity.getContactEmail()),
                entity.getContactPhone() == null ? null : ContactPhone.of(entity.getContactPhone()),
                entity.getDateOfBirth() == null ? null : DateOfBirth.of(entity.getDateOfBirth())
        );

        PrimaryOrganizationId primaryOrganizationId = entity.getPrimaryOrganizationId() == null
                ? null
                : PrimaryOrganizationId.of(entity.getPrimaryOrganizationId());

        return Patient.reconstitute(
                new PatientId(entity.getPatientId()),
                new TenantId(entity.getTenantId()),
                demographics,
                externalIdentifiers,
                primaryOrganizationId,
                PatientStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public PatientEntity toEntity(Patient patient, boolean isNew) {
        PatientEntity entity = new PatientEntity();
        entity.setNewEntity(isNew);
        entity.setPatientId(patient.id().value());
        entity.setTenantId(patient.tenantId().value());
        entity.setPrimaryOrganizationId(
                patient.primaryOrganizationId().map(PrimaryOrganizationId::value).orElse(null)
        );
        entity.setDisplayName(patient.demographics().displayName().value());
        entity.setContactEmail(patient.demographics().email().map(ContactEmail::value).orElse(null));
        entity.setContactPhone(patient.demographics().phone().map(ContactPhone::value).orElse(null));
        entity.setDateOfBirth(patient.demographics().dateOfBirth().map(DateOfBirth::value).orElse(null));
        entity.setStatus(patient.status().name());
        entity.setCreatedAt(patient.createdAt());
        entity.setUpdatedAt(patient.updatedAt());
        return entity;
    }
}
