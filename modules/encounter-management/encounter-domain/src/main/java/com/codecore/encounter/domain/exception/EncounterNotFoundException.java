package com.codecore.encounter.domain.exception;

/**
 * Raised when an Encounter cannot be resolved for the requested identity/tenant scope.
 */
public final class EncounterNotFoundException extends EncounterDomainException {

    public EncounterNotFoundException(String message) {
        super(message);
    }
}
