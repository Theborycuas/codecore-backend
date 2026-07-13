package com.codecore.access.interfaces.http;

import com.codecore.access.domain.exception.ActiveMembershipAlreadyExistsException;
import com.codecore.access.domain.exception.InvalidDomainValueException;
import com.codecore.access.domain.exception.InvalidInvitationStateException;
import com.codecore.access.domain.exception.InvitationNotFoundException;
import com.codecore.access.domain.exception.InviterMembershipNotFoundException;
import com.codecore.access.domain.exception.PendingInvitationAlreadyExistsException;
import com.codecore.access.domain.exception.SystemRoleNotFoundException;
import com.codecore.iam.domain.exception.AuthorizationDeniedException;
import com.codecore.iam.domain.exception.MembershipAlreadyExistsException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice(basePackages = "com.codecore.access.interfaces.http")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccessHttpExceptionHandler {

    @ExceptionHandler(InvitationNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<Void> handleInvitationNotFound(InvitationNotFoundException ex) {
        return Mono.empty();
    }

    @ExceptionHandler(InviterMembershipNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<Void> handleInviterNotFound(InviterMembershipNotFoundException ex) {
        return Mono.empty();
    }

    @ExceptionHandler(SystemRoleNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<Void> handleSystemRoleNotFound(SystemRoleNotFoundException ex) {
        return Mono.empty();
    }

    @ExceptionHandler({
            ActiveMembershipAlreadyExistsException.class,
            PendingInvitationAlreadyExistsException.class,
            MembershipAlreadyExistsException.class
    })
    @ResponseStatus(HttpStatus.CONFLICT)
    public Mono<Void> handleConflict(RuntimeException ex) {
        return Mono.empty();
    }

    @ExceptionHandler(InvalidDomainValueException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Void> handleInvalidDomain(InvalidDomainValueException ex) {
        return Mono.empty();
    }

    @ExceptionHandler(InvalidInvitationStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Void> handleInvalidInvitationState(InvalidInvitationStateException ex) {
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
