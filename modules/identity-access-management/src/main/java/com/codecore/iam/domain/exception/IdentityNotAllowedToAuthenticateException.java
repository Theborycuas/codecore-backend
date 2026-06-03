package com.codecore.iam.domain.exception;

/**
 * Identity exists but its lifecycle state does not allow authentication.
 */
public class IdentityNotAllowedToAuthenticateException extends IamDomainException {

    public IdentityNotAllowedToAuthenticateException(String message) {
        super(message);
    }
}
