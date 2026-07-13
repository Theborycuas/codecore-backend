package com.codecore.access.domain.exception;

/**
 * Raised when the invited system role code does not exist for the tenant.
 */
public final class SystemRoleNotFoundException extends AccessDomainException {

    public SystemRoleNotFoundException(String message) {
        super(message);
    }
}
