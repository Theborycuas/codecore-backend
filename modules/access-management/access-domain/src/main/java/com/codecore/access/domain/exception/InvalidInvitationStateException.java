package com.codecore.access.domain.exception;

/**
 * Raised when an invitation lifecycle transition is not allowed.
 */
public final class InvalidInvitationStateException extends AccessDomainException {

    public InvalidInvitationStateException(String message) {
        super(message);
    }
}
