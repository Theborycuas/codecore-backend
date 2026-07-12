package com.codecore.inventory.domain.exception;

/**
 * Base exception for Inventory (Item) domain rule violations.
 */
public class ItemDomainException extends RuntimeException {

    public ItemDomainException(String message) {
        super(message);
    }
}
