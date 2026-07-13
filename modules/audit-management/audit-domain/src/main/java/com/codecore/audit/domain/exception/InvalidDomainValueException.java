package com.codecore.audit.domain.exception;

/**
 * Raised when a value object or factory argument violates domain constraints (ADR-020).
 */
public class InvalidDomainValueException extends AuditDomainException {

    public InvalidDomainValueException(String message) {
        super(message);
    }
}
