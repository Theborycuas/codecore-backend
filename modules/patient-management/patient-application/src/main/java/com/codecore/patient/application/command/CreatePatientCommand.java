package com.codecore.patient.application.command;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreatePatientCommand(
        String displayName,
        String contactEmail,
        String contactPhone,
        LocalDate dateOfBirth,
        UUID primaryOrganizationId,
        List<ExternalIdentifierInput> externalIdentifiers
) {

    public record ExternalIdentifierInput(String type, String value) {
    }
}
