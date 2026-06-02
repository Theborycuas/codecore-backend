package com.codecore.iam.interfaces.http;

import com.codecore.iam.domain.exception.IdentityAlreadyExistsException;
import com.codecore.iam.domain.exception.InvalidDomainValueException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

/**
 * Minimal HTTP error mapping for IAM adapters (no generic error framework).
 */
@RestControllerAdvice(basePackages = "com.codecore.iam.interfaces.http")
public class IamHttpExceptionHandler {

    @ExceptionHandler(IdentityAlreadyExistsException.class)
    public Mono<ResponseEntity<Void>> handleDuplicate(IdentityAlreadyExistsException ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build());
    }

    @ExceptionHandler(InvalidDomainValueException.class)
    public Mono<ResponseEntity<Void>> handleInvalidDomain(InvalidDomainValueException ex) {
        return Mono.just(ResponseEntity.badRequest().build());
    }
}
