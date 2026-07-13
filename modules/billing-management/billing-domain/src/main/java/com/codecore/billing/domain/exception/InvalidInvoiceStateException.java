package com.codecore.billing.domain.exception;

/**
 * Raised when an invoice lifecycle or mutation transition is not allowed.
 */
public final class InvalidInvoiceStateException extends InvoiceDomainException {

    public InvalidInvoiceStateException(String message) {
        super(message);
    }
}
