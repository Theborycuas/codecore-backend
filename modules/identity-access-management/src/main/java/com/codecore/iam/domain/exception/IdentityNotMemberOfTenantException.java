package com.codecore.iam.domain.exception;

/**
 * Raised when an identity has no active membership for the requested tenant.
 */
public class IdentityNotMemberOfTenantException extends IamDomainException {

    public IdentityNotMemberOfTenantException(String message) {
        super(message);
    }
}
