package com.codecore.access.domain.exception;

/**
 * Raised when a PENDING invitation already exists for the same email + tenant.
 */
public final class PendingInvitationAlreadyExistsException extends AccessDomainException {

    public PendingInvitationAlreadyExistsException(String message) {
        super(message);
    }
}
