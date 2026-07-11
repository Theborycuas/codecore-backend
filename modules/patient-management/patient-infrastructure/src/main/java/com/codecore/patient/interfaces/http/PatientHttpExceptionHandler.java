package com.codecore.patient.interfaces.http;

import com.codecore.iam.domain.exception.AuthorizationDeniedException;
import com.codecore.patient.domain.exception.InvalidDomainValueException;
import com.codecore.patient.domain.exception.InvalidPatientStateException;
import com.codecore.patient.domain.exception.PatientNotFoundException;
import com.codecore.patient.domain.exception.PrimaryOrganizationNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice(basePackages = "com.codecore.patient.interfaces.http")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PatientHttpExceptionHandler {

    @ExceptionHandler(PatientNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<Void> handlePatientNotFound(PatientNotFoundException ex) {
        return Mono.empty();
    }

    @ExceptionHandler(PrimaryOrganizationNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<Void> handlePrimaryOrganizationNotFound(PrimaryOrganizationNotFoundException ex) {
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

    @ExceptionHandler(InvalidPatientStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Void> handleInvalidPatientState(InvalidPatientStateException ex) {
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
