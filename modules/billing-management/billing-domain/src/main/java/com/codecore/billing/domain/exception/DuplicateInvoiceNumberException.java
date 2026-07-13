package com.codecore.billing.domain.exception;

/**
 * Raised when the (tenantId, invoiceNumber) soft-uniqueness invariant is violated
 * (ADR-017 §5 — invoiceNumber is optional but soft-unique per tenant when present).
 */
public final class DuplicateInvoiceNumberException extends InvoiceDomainException {

    public DuplicateInvoiceNumberException(String message) {
        super(message);
    }
}
