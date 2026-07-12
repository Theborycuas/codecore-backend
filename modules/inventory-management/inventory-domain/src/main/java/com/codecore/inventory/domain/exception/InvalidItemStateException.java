package com.codecore.inventory.domain.exception;

/**
 * Raised when an item lifecycle or mutation transition is not allowed.
 */
public final class InvalidItemStateException extends ItemDomainException {

    public InvalidItemStateException(String message) {
        super(message);
    }
}
