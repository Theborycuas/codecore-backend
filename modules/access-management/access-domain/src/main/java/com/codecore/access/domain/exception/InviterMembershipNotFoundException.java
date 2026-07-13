package com.codecore.access.domain.exception;

/**
 * Raised when the inviting membership is missing, inactive, or not in the tenant.
 */
public final class InviterMembershipNotFoundException extends AccessDomainException {

    public InviterMembershipNotFoundException(String message) {
        super(message);
    }
}
