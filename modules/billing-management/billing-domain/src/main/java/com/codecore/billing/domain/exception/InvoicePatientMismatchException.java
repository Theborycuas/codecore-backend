package com.codecore.billing.domain.exception;

/**
 * Raised when a line's Encounter patientId does not match the Invoice bill-to Patient
 * (ADR-017 §10.7 coherence invariant).
 */
public final class InvoicePatientMismatchException extends InvoiceDomainException {

    public InvoicePatientMismatchException(String message) {
        super(message);
    }
}
