package com.codecore.iam.domain.exception;

/**
 * Raised when authorization cannot be granted (missing membership, role, or permission).
 */
public class AuthorizationDeniedException extends IamDomainException {

    public AuthorizationDeniedException(String message) {
        super(message);
    }
}
