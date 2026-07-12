package com.codecore.encounter.interfaces.http;

import com.codecore.encounter.domain.exception.EncounterCoherenceException;
import com.codecore.encounter.domain.exception.EncounterNotFoundException;
import com.codecore.encounter.domain.exception.EncounterReferenceNotFoundException;
import com.codecore.encounter.domain.exception.InvalidDomainValueException;
import com.codecore.encounter.domain.exception.InvalidEncounterStateException;
import com.codecore.iam.domain.exception.AuthorizationDeniedException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice(basePackages = "com.codecore.encounter.interfaces.http")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EncounterHttpExceptionHandler {

    @ExceptionHandler(EncounterNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<Void> handleEncounterNotFound(EncounterNotFoundException ex) {
        return Mono.empty();
    }

    @ExceptionHandler(EncounterReferenceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<Void> handleEncounterReferenceNotFound(EncounterReferenceNotFoundException ex) {
        return Mono.empty();
    }

    @ExceptionHandler(EncounterCoherenceException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Mono<Void> handleEncounterCoherence(EncounterCoherenceException ex) {
        return Mono.empty();
    }

    @ExceptionHandler(InvalidEncounterStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Mono<Void> handleInvalidEncounterState(InvalidEncounterStateException ex) {
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
