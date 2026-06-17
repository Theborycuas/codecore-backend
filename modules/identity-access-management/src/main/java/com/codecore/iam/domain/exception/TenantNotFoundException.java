package com.codecore.iam.domain.exception;

public class TenantNotFoundException extends IamDomainException {

    public TenantNotFoundException(String message) {
        super(message);
    }
}
