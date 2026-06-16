package com.codecore.iam.interfaces.http;

import com.codecore.iam.domain.exception.AuthorizationDeniedException;
import com.codecore.iam.domain.exception.IdentityAlreadyExistsException;
import com.codecore.iam.domain.exception.TenantAlreadyExistsException;
import com.codecore.iam.domain.exception.IdentityNotAllowedToAuthenticateException;
import com.codecore.iam.domain.exception.IdentityNotMemberOfTenantException;
import com.codecore.iam.domain.exception.InvalidCredentialsException;
import com.codecore.iam.domain.exception.InvalidDomainValueException;
import com.codecore.iam.domain.valueobject.IdentityStatus;
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

    @ExceptionHandler(TenantAlreadyExistsException.class)
    public Mono<ResponseEntity<Void>> handleTenantDuplicate(TenantAlreadyExistsException ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build());
    }

    @ExceptionHandler(InvalidDomainValueException.class)
    public Mono<ResponseEntity<Void>> handleInvalidDomain(InvalidDomainValueException ex) {
        return Mono.just(ResponseEntity.badRequest().build());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public Mono<ResponseEntity<Void>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @ExceptionHandler(IdentityNotAllowedToAuthenticateException.class)
    public Mono<ResponseEntity<Void>> handleNotAllowed(IdentityNotAllowedToAuthenticateException ex) {
        HttpStatus status = ex.status() == IdentityStatus.LOCKED
                ? HttpStatus.LOCKED
                : HttpStatus.FORBIDDEN;
        return Mono.just(ResponseEntity.status(status).build());
    }

    @ExceptionHandler(IdentityNotMemberOfTenantException.class)
    public Mono<ResponseEntity<Void>> handleNotMember(IdentityNotMemberOfTenantException ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public Mono<ResponseEntity<Void>> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }
}
