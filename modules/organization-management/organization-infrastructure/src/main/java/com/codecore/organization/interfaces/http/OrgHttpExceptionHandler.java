package com.codecore.organization.interfaces.http;

import com.codecore.iam.domain.exception.AuthorizationDeniedException;
import com.codecore.organization.domain.exception.InvalidDomainValueException;
import com.codecore.organization.domain.exception.InvalidOfficeStateException;
import com.codecore.organization.domain.exception.InvalidOrganizationStateException;
import com.codecore.organization.domain.exception.InvalidStaffAssignmentScopeException;
import com.codecore.organization.domain.exception.MembershipNotInTenantException;
import com.codecore.organization.domain.exception.OfficeAlreadyExistsException;
import com.codecore.organization.domain.exception.OfficeNotFoundException;
import com.codecore.organization.domain.exception.OrganizationAlreadyExistsException;
import com.codecore.organization.domain.exception.OrganizationHasActiveOfficesException;
import com.codecore.organization.domain.exception.OrganizationNotActiveException;
import com.codecore.organization.domain.exception.OrganizationNotFoundException;
import com.codecore.organization.domain.exception.StaffAssignmentAlreadyExistsException;
import com.codecore.organization.domain.exception.StaffAssignmentNotFoundException;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice(basePackages = "com.codecore.organization.interfaces.http")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OrgHttpExceptionHandler {

    @ExceptionHandler(OrganizationNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<Void> handleOrganizationNotFound(OrganizationNotFoundException ex) {
        return Mono.empty();
    }

    @ExceptionHandler(OfficeNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<Void> handleOfficeNotFound(OfficeNotFoundException ex) {
        return Mono.empty();
    }

    @ExceptionHandler(StaffAssignmentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<Void> handleStaffAssignmentNotFound(StaffAssignmentNotFoundException ex) {
        return Mono.empty();
    }

    @ExceptionHandler(MembershipNotInTenantException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<Void> handleMembershipNotInTenant(MembershipNotInTenantException ex) {
        return Mono.empty();
    }

    @ExceptionHandler(OrganizationAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Mono<Void> handleOrganizationAlreadyExists(OrganizationAlreadyExistsException ex) {
        return Mono.empty();
    }

    @ExceptionHandler(OfficeAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Mono<Void> handleOfficeAlreadyExists(OfficeAlreadyExistsException ex) {
        return Mono.empty();
    }

    @ExceptionHandler(StaffAssignmentAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Mono<Void> handleStaffAssignmentAlreadyExists(StaffAssignmentAlreadyExistsException ex) {
        return Mono.empty();
    }

    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Mono<Void> handleDuplicateKey(DuplicateKeyException ex) {
        return Mono.empty();
    }

    @ExceptionHandler(OrganizationHasActiveOfficesException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Mono<Void> handleOrganizationHasActiveOffices(OrganizationHasActiveOfficesException ex) {
        return Mono.empty();
    }

    @ExceptionHandler(OrganizationNotActiveException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Mono<Void> handleOrganizationNotActive(OrganizationNotActiveException ex) {
        return Mono.empty();
    }

    @ExceptionHandler(InvalidDomainValueException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Void> handleInvalidDomain(InvalidDomainValueException ex) {
        return Mono.empty();
    }

    @ExceptionHandler(InvalidOrganizationStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Void> handleInvalidOrganizationState(InvalidOrganizationStateException ex) {
        return Mono.empty();
    }

    @ExceptionHandler(InvalidOfficeStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Void> handleInvalidOfficeState(InvalidOfficeStateException ex) {
        return Mono.empty();
    }

    @ExceptionHandler(InvalidStaffAssignmentScopeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Void> handleInvalidStaffAssignmentScope(InvalidStaffAssignmentScopeException ex) {
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
