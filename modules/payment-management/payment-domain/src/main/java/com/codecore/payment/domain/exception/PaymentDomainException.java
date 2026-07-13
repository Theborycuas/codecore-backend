package com.codecore.payment.domain.exception;

/**
 * Base exception for Payments (Payment) domain rule violations.
 */
public class PaymentDomainException extends RuntimeException {

    public PaymentDomainException(String message) {
        super(message);
    }
}
