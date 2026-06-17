package com.codecore.iam.domain.exception;

public class RoleAlreadyExistsException extends IamDomainException {

    public RoleAlreadyExistsException(String message) {
        super(message);
    }
}
