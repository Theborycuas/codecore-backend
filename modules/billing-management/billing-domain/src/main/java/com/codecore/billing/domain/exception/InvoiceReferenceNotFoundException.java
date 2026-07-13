package com.codecore.billing.domain.exception;

/**
 * Raised when a cross-BC reference (issuer Organization, bill-to Patient/Organization,
 * line Item, or line Encounter) is not found, not ACTIVE, or not linkable in the tenant
 * (ADR-013 / ADR-017).
 */
public final class InvoiceReferenceNotFoundException extends InvoiceDomainException {

    public InvoiceReferenceNotFoundException(String message) {
        super(message);
    }
}
