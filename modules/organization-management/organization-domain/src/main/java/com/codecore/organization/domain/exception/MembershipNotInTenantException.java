package com.codecore.organization.domain.exception;

public final class MembershipNotInTenantException extends OrganizationDomainException {

    public MembershipNotInTenantException(String message) {
        super(message);
    }
}
