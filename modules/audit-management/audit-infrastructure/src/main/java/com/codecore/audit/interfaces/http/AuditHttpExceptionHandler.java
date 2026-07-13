package com.codecore.audit.interfaces.http;

import com.codecore.audit.domain.exception.ActorMembershipNotFoundException;
import com.codecore.audit.domain.exception.AuditEntryNotFoundException;
import com.codecore.audit.domain.exception.InvalidDomainValueException;
import com.codecore.iam.domain.exception.AuthorizationDeniedException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice(basePackages = "com.codecore.audit.interfaces.http")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuditHttpExceptionHandler {

    @ExceptionHandler(AuditEntryNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<Void> handleAuditEntryNotFound(AuditEntryNotFoundException ex) {
        return Mono.empty();
    }

    @ExceptionHandler(ActorMembershipNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Void> handleActorMembershipNotFound(ActorMembershipNotFoundException ex) {
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
