package com.codecore.iam.domain.exception;

/**
 * Authentication failed due to unknown identity or invalid password (same surface for anti-enumeration).
 */
public class InvalidCredentialsException extends IamDomainException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
