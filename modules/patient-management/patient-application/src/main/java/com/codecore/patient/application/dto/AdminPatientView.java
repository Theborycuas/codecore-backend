package com.codecore.patient.application.dto;

import com.codecore.patient.domain.valueobject.ExternalIdentifier;
import com.codecore.patient.domain.valueobject.PatientId;
import com.codecore.patient.domain.valueobject.PatientStatus;
import com.codecore.patient.domain.valueobject.PrimaryOrganizationId;
import com.codecore.patient.domain.valueobject.TenantId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AdminPatientView(
        PatientId id,
        TenantId tenantId,
        String displayName,
        String contactEmail,
        String contactPhone,
        LocalDate dateOfBirth,
        PrimaryOrganizationId primaryOrganizationId,
        List<ExternalIdentifierItem> externalIdentifiers,
        PatientStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public record ExternalIdentifierItem(String type, String value) {

        public static ExternalIdentifierItem from(ExternalIdentifier identifier) {
            return new ExternalIdentifierItem(identifier.type().value(), identifier.value());
        }
    }

    public UUID primaryOrganizationUuid() {
        return primaryOrganizationId == null ? null : primaryOrganizationId.value();
    }
}
