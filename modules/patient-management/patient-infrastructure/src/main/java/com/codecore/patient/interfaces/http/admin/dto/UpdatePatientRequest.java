package com.codecore.patient.interfaces.http.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UpdatePatientRequest(
        @NotBlank @Size(max = 200) String displayName,
        @Size(max = 320) String contactEmail,
        @Size(max = 32) String contactPhone,
        LocalDate dateOfBirth,
        UUID primaryOrganizationId,
        @Valid List<ExternalIdentifierRequest> externalIdentifiers
) {
}
