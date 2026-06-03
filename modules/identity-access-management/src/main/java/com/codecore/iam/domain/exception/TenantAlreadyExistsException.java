package com.codecore.iam.domain.exception;

/**
 * Raised when a tenant with the same name already exists.
 */
public class TenantAlreadyExistsException extends IamDomainException {

    public TenantAlreadyExistsException(String message) {
        super(message);
    }
}
