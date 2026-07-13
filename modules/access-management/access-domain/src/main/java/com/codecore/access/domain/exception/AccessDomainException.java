package com.codecore.access.domain.exception;

/**
 * Base exception for Access (Invitation) domain rule violations.
 */
public class AccessDomainException extends RuntimeException {

    public AccessDomainException(String message) {
        super(message);
    }
}
