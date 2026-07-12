package com.codecore.encounter.domain.exception;

/**
 * Base exception for Clinical Records (Encounter) domain rule violations.
 */
public class EncounterDomainException extends RuntimeException {

    public EncounterDomainException(String message) {
        super(message);
    }
}
