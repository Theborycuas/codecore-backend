package com.codecore.billing.domain.exception;

/**
 * Raised when an Invoice cannot be found in the current tenant context.
 */
public final class InvoiceNotFoundException extends InvoiceDomainException {

    public InvoiceNotFoundException(String message) {
        super(message);
    }
}
