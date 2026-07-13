package com.codecore.audit.domain.exception;

/**
 * Raised when an AuditEntry cannot be found in the current tenant context (ADR-020).
 */
public class AuditEntryNotFoundException extends AuditDomainException {

    public AuditEntryNotFoundException(String message) {
        super(message);
    }
}
