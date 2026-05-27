package com.codecore.iam.domain.exception;

/**
 * Base type for IAM domain rule violations.
 */
public class IamDomainException extends RuntimeException {

    public IamDomainException(String message) {
        super(message);
    }
}
