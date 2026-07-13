package com.codecore.access.domain.exception;

/**
 * Raised when an Invitation cannot be found in the current tenant context.
 */
public final class InvitationNotFoundException extends AccessDomainException {

    public InvitationNotFoundException(String message) {
        super(message);
    }
}
