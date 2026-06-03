package com.codecore.iam.domain.exception;

/**
 * Access token is missing, malformed, has invalid signature/issuer, or lacks required claims.
 */
public class InvalidTokenException extends IamDomainException {

    public InvalidTokenException(String message) {
        super(message);
    }
}
