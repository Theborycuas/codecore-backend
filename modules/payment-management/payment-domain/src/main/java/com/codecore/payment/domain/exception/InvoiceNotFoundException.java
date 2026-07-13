package com.codecore.payment.domain.exception;

/**
 * Raised on Payment create when the referenced Invoice does not exist, is not in the current
 * tenant, or is not {@code ISSUED} (ADR-018 — validated via
 * {@code InvoiceReferencePort.existsIssuedByIdAndTenant}, mapped to HTTP 404).
 */
public final class InvoiceNotFoundException extends PaymentDomainException {

    public InvoiceNotFoundException(String message) {
        super(message);
    }
}
