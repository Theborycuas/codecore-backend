package com.codecore.encounter.domain.exception;

/**
 * Raised when a cross-BC reference is missing or not usable for Encounter writes (ADR-013).
 */
public final class EncounterReferenceNotFoundException extends EncounterDomainException {

    public EncounterReferenceNotFoundException(String message) {
        super(message);
    }
}
