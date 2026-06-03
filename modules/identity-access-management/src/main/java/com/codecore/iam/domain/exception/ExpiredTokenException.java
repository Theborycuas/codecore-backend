package com.codecore.iam.domain.exception;

/**
 * Access token signature is valid but the token is past its expiration time.
 */
public class ExpiredTokenException extends IamDomainException {

    public ExpiredTokenException(String message) {
        super(message);
    }
}
