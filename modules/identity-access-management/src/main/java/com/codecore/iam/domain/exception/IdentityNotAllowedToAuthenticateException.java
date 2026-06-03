package com.codecore.iam.domain.exception;

import com.codecore.iam.domain.valueobject.IdentityStatus;

import java.util.Objects;

/**
 * Identity exists but its lifecycle state does not allow authentication.
 */
public class IdentityNotAllowedToAuthenticateException extends IamDomainException {

    private final IdentityStatus status;

    public IdentityNotAllowedToAuthenticateException(String message, IdentityStatus status) {
        super(message);
        this.status = Objects.requireNonNull(status, "status");
    }

    public IdentityStatus status() {
        return status;
    }
}
