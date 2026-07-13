package com.codecore.audit.domain.exception;

/**
 * Base unchecked exception for the Audit bounded context (ADR-020).
 */
public class AuditDomainException extends RuntimeException {

    public AuditDomainException(String message) {
        super(message);
    }

    public AuditDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
