package com.codecore.organization.domain.exception;

/**
 * Base exception for Organization Management domain rule violations.
 */
public class OrganizationDomainException extends RuntimeException {

    public OrganizationDomainException(String message) {
        super(message);
    }
}
