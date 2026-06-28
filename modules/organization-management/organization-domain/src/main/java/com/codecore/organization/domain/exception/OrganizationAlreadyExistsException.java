package com.codecore.organization.domain.exception;

public final class OrganizationAlreadyExistsException extends OrganizationDomainException {

    public OrganizationAlreadyExistsException(String message) {
        super(message);
    }
}
