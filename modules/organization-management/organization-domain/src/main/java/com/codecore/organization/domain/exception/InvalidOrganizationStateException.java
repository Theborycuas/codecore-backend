package com.codecore.organization.domain.exception;

/**
 * Raised when an organization lifecycle transition is not allowed.
 */
public final class InvalidOrganizationStateException extends OrganizationDomainException {

    public InvalidOrganizationStateException(String message) {
        super(message);
    }
}
