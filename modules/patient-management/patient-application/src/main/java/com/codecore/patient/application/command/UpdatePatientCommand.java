package com.codecore.patient.application.command;

import com.codecore.patient.domain.valueobject.PatientId;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Full replace of mutable registry fields (PUT semantics).
 * {@code primaryOrganizationId == null} clears the optional primary organization.
 */
public record UpdatePatientCommand(
        PatientId patientId,
        String displayName,
        String contactEmail,
        String contactPhone,
        LocalDate dateOfBirth,
        UUID primaryOrganizationId,
        List<CreatePatientCommand.ExternalIdentifierInput> externalIdentifiers
) {
}
