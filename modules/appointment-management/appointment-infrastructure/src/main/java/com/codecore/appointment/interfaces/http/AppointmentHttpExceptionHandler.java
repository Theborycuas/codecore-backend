package com.codecore.appointment.interfaces.http;

import com.codecore.appointment.domain.exception.AppointmentCoherenceException;
import com.codecore.appointment.domain.exception.AppointmentNotFoundException;
import com.codecore.appointment.domain.exception.AppointmentReferenceNotFoundException;
import com.codecore.appointment.domain.exception.InvalidAppointmentStateException;
import com.codecore.appointment.domain.exception.InvalidDomainValueException;
import com.codecore.iam.domain.exception.AuthorizationDeniedException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice(basePackages = "com.codecore.appointment.interfaces.http")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AppointmentHttpExceptionHandler {

    @ExceptionHandler(AppointmentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<Void> handleAppointmentNotFound(AppointmentNotFoundException ex) {
        return Mono.empty();
    }

    @ExceptionHandler(AppointmentReferenceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<Void> handleAppointmentReferenceNotFound(AppointmentReferenceNotFoundException ex) {
        return Mono.empty();
    }

    @ExceptionHandler(AppointmentCoherenceException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Mono<Void> handleAppointmentCoherence(AppointmentCoherenceException ex) {
        return Mono.empty();
    }

    @ExceptionHandler(InvalidAppointmentStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Mono<Void> handleInvalidAppointmentState(InvalidAppointmentStateException ex) {
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
