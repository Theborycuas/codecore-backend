package com.codecore.audit.domain.exception;

/**
 * Raised when an optional actor membership is present but not ACTIVE in the tenant (ADR-020).
 */
public class ActorMembershipNotFoundException extends AuditDomainException {

    public ActorMembershipNotFoundException(String message) {
        super(message);
    }
}
