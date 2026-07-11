package com.codecore.patient.domain.exception;

/**
 * Raised when optional PrimaryOrganizationId is not ACTIVE in the tenant (ADR-012 · ADR-013).
 */
public final class PrimaryOrganizationNotFoundException extends PatientDomainException {

    public PrimaryOrganizationNotFoundException(String message) {
        super(message);
    }
}
