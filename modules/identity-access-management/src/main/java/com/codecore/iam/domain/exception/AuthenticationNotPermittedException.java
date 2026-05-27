package com.codecore.iam.domain.exception;

public class AuthenticationNotPermittedException extends IamDomainException {

    public AuthenticationNotPermittedException(String message) {
        super(message);
    }
}
