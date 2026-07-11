package com.codecore.patient.application.query;

import java.util.Objects;
import java.util.UUID;

/**
 * List filters for Patient administration (PASO 17.5.1).
 */
public record PatientListQuery(
        PatientListFilter status,
        String q,
        UUID primaryOrganizationId,
        String externalIdentifierType,
        String externalIdentifierValue
) {

    public PatientListQuery {
        status = Objects.requireNonNull(status, "status");
        if (q != null && q.isBlank()) {
            q = null;
        }
        if (externalIdentifierType != null && externalIdentifierType.isBlank()) {
            externalIdentifierType = null;
        }
        if (externalIdentifierValue != null && externalIdentifierValue.isBlank()) {
            externalIdentifierValue = null;
        }
    }

    public static PatientListQuery of(
            String status,
            String q,
            UUID primaryOrganizationId,
            String externalIdentifierType,
            String externalIdentifierValue
    ) {
        return new PatientListQuery(
                PatientListFilter.parse(status),
                q,
                primaryOrganizationId,
                externalIdentifierType,
                externalIdentifierValue
        );
    }
}
