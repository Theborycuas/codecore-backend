package com.codecore.billing.domain.exception;

/**
 * Base exception for Billing (Invoice) domain rule violations.
 */
public class InvoiceDomainException extends RuntimeException {

    public InvoiceDomainException(String message) {
        super(message);
    }
}
