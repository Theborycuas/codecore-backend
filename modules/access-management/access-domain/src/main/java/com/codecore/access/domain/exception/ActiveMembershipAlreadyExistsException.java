package com.codecore.access.domain.exception;

/**
 * Raised when the invitee already has an ACTIVE membership in the tenant.
 */
public final class ActiveMembershipAlreadyExistsException extends AccessDomainException {

    public ActiveMembershipAlreadyExistsException(String message) {
        super(message);
    }
}
