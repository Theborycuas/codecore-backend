package com.codecore.iam.domain.exception;

/**
 * Raised when {@code tenant_id + normalized_email} already exists.
 */
public class IdentityAlreadyExistsException extends IamDomainException {

    public IdentityAlreadyExistsException(String message) {
        super(message);
    }
}
