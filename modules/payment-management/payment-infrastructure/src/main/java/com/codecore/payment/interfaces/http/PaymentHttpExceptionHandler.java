package com.codecore.payment.interfaces.http;

import com.codecore.iam.domain.exception.AuthorizationDeniedException;
import com.codecore.payment.domain.exception.InvalidDomainValueException;
import com.codecore.payment.domain.exception.InvalidPaymentStateException;
import com.codecore.payment.domain.exception.InvoiceNotFoundException;
import com.codecore.payment.domain.exception.PaymentNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice(basePackages = "com.codecore.payment.interfaces.http")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PaymentHttpExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<Void> handlePaymentNotFound(PaymentNotFoundException ex) {
        return Mono.empty();
    }

    @ExceptionHandler(InvoiceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<Void> handleInvoiceNotFound(InvoiceNotFoundException ex) {
        return Mono.empty();
    }

    @ExceptionHandler(InvalidDomainValueException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Void> handleInvalidDomain(InvalidDomainValueException ex) {
        return Mono.empty();
    }

    @ExceptionHandler(InvalidPaymentStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Void> handleInvalidPaymentState(InvalidPaymentStateException ex) {
        return Mono.empty();
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Mono<Void> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        return Mono.empty();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Void> handleIllegalArgument(IllegalArgumentException ex) {
        return Mono.empty();
    }
}
