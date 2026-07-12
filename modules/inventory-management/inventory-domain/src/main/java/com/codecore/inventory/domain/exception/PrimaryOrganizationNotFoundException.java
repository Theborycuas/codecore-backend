package com.codecore.inventory.domain.exception;

/**
 * Raised when optional PrimaryOrganizationId is not ACTIVE in the tenant (ADR-016 · ADR-013).
 */
public final class PrimaryOrganizationNotFoundException extends ItemDomainException {

    public PrimaryOrganizationNotFoundException(String message) {
        super(message);
    }
}
