package com.codecore.billing.interfaces.http;

import com.codecore.billing.domain.exception.DuplicateInvoiceNumberException;
import com.codecore.billing.domain.exception.InvalidDomainValueException;
import com.codecore.billing.domain.exception.InvalidInvoiceStateException;
import com.codecore.billing.domain.exception.InvoiceNotFoundException;
import com.codecore.billing.domain.exception.InvoicePatientMismatchException;
import com.codecore.billing.domain.exception.InvoiceReferenceNotFoundException;
import com.codecore.iam.domain.exception.AuthorizationDeniedException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice(basePackages = "com.codecore.billing.interfaces.http")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InvoiceHttpExceptionHandler {

    @ExceptionHandler(InvoiceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<Void> handleInvoiceNotFound(InvoiceNotFoundException ex) {
        return Mono.empty();
    }

    @ExceptionHandler(InvoiceReferenceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<Void> handleInvoiceReferenceNotFound(InvoiceReferenceNotFoundException ex) {
        return Mono.empty();
    }

    @ExceptionHandler(DuplicateInvoiceNumberException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Mono<Void> handleDuplicateInvoiceNumber(DuplicateInvoiceNumberException ex) {
        return Mono.empty();
    }

    @ExceptionHandler(InvoicePatientMismatchException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Mono<Void> handleInvoicePatientMismatch(InvoicePatientMismatchException ex) {
        return Mono.empty();
    }

    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Mono<Void> handleDuplicateKey(DuplicateKeyException ex) {
        return Mono.empty();
    }

    @ExceptionHandler(InvalidDomainValueException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Void> handleInvalidDomain(InvalidDomainValueException ex) {
        return Mono.empty();
    }

    @ExceptionHandler(InvalidInvoiceStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Void> handleInvalidInvoiceState(InvalidInvoiceStateException ex) {
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
