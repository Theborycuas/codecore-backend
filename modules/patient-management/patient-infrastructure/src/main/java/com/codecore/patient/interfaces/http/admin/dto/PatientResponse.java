package com.codecore.patient.interfaces.http.admin.dto;

import com.codecore.patient.application.dto.AdminPatientView;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PatientResponse(
        UUID id,
        String displayName,
        String contactEmail,
        String contactPhone,
        LocalDate dateOfBirth,
        UUID primaryOrganizationId,
        List<ExternalIdentifierResponse> externalIdentifiers,
        String status,
        Instant createdAt,
        Instant updatedAt
) {

    public static PatientResponse from(AdminPatientView view) {
        return new PatientResponse(
                view.id().value(),
                view.displayName(),
                view.contactEmail(),
                view.contactPhone(),
                view.dateOfBirth(),
                view.primaryOrganizationUuid(),
                view.externalIdentifiers().stream()
                        .map(item -> new ExternalIdentifierResponse(item.type(), item.value()))
                        .toList(),
                view.status().name(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    public record ExternalIdentifierResponse(String type, String value) {
    }
}
