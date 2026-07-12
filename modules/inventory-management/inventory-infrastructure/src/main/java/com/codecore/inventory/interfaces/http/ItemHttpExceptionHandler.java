package com.codecore.inventory.interfaces.http;

import com.codecore.iam.domain.exception.AuthorizationDeniedException;
import com.codecore.inventory.domain.exception.InvalidDomainValueException;
import com.codecore.inventory.domain.exception.InvalidItemStateException;
import com.codecore.inventory.domain.exception.ItemNotFoundException;
import com.codecore.inventory.domain.exception.PrimaryOrganizationNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice(basePackages = "com.codecore.inventory.interfaces.http")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ItemHttpExceptionHandler {

    @ExceptionHandler(ItemNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<Void> handleItemNotFound(ItemNotFoundException ex) {
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

    @ExceptionHandler(InvalidItemStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Void> handleInvalidItemState(InvalidItemStateException ex) {
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
