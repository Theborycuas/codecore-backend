package com.codecore.iam.domain.exception;

public class RoleInUseException extends IamDomainException {

    public RoleInUseException(String message) {
        super(message);
    }
}
