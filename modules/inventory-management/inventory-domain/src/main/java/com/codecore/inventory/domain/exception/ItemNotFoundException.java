package com.codecore.inventory.domain.exception;

/**
 * Raised when an Item cannot be found in the current tenant context.
 */
public final class ItemNotFoundException extends ItemDomainException {

    public ItemNotFoundException(String message) {
        super(message);
    }
}
