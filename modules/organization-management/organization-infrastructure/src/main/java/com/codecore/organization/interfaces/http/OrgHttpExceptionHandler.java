package com.codecore.organization.interfaces.http;

import com.codecore.organization.domain.exception.InvalidDomainValueException;
import com.codecore.organization.domain.exception.InvalidStaffAssignmentScopeException;
import com.codecore.organization.domain.exception.MembershipNotInTenantException;
import com.codecore.organization.domain.exception.StaffAssignmentAlreadyExistsException;
import com.codecore.organization.domain.exception.StaffAssignmentNotFoundException;
import com.codecore.organization.domain.exception.InvalidOfficeStateException;
import com.codecore.organization.domain.exception.InvalidOrganizationStateException;
import com.codecore.organization.domain.exception.OfficeAlreadyExistsException;
import com.codecore.organization.domain.exception.OfficeNotFoundException;
import com.codecore.organization.domain.exception.OrganizationAlreadyExistsException;
import com.codecore.organization.domain.exception.OrganizationHasActiveOfficesException;
import com.codecore.organization.domain.exception.OrganizationNotActiveException;
import com.codecore.organization.domain.exception.OrganizationNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice(basePackages = "com.codecore.organization.interfaces.http")
public class OrgHttpExceptionHandler {

    @ExceptionHandler(OrganizationNotFoundException.class)
    public Mono<ResponseEntity<Void>> handleOrganizationNotFound(OrganizationNotFoundException ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @ExceptionHandler(OfficeNotFoundException.class)
    public Mono<ResponseEntity<Void>> handleOfficeNotFound(OfficeNotFoundException ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @ExceptionHandler(StaffAssignmentNotFoundException.class)
    public Mono<ResponseEntity<Void>> handleStaffAssignmentNotFound(StaffAssignmentNotFoundException ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @ExceptionHandler(MembershipNotInTenantException.class)
    public Mono<ResponseEntity<Void>> handleMembershipNotInTenant(MembershipNotInTenantException ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @ExceptionHandler(OrganizationAlreadyExistsException.class)
    public Mono<ResponseEntity<Void>> handleOrganizationAlreadyExists(OrganizationAlreadyExistsException ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build());
    }

    @ExceptionHandler(OfficeAlreadyExistsException.class)
    public Mono<ResponseEntity<Void>> handleOfficeAlreadyExists(OfficeAlreadyExistsException ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build());
    }

    @ExceptionHandler(StaffAssignmentAlreadyExistsException.class)
    public Mono<ResponseEntity<Void>> handleStaffAssignmentAlreadyExists(
            StaffAssignmentAlreadyExistsException ex
    ) {
        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build());
    }

    @ExceptionHandler(OrganizationHasActiveOfficesException.class)
    public Mono<ResponseEntity<Void>> handleOrganizationHasActiveOffices(
            OrganizationHasActiveOfficesException ex
    ) {
        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build());
    }

    @ExceptionHandler(OrganizationNotActiveException.class)
    public Mono<ResponseEntity<Void>> handleOrganizationNotActive(OrganizationNotActiveException ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build());
    }

    @ExceptionHandler(InvalidDomainValueException.class)
    public Mono<ResponseEntity<Void>> handleInvalidDomain(InvalidDomainValueException ex) {
        return Mono.just(ResponseEntity.badRequest().build());
    }

    @ExceptionHandler(InvalidOrganizationStateException.class)
    public Mono<ResponseEntity<Void>> handleInvalidOrganizationState(InvalidOrganizationStateException ex) {
        return Mono.just(ResponseEntity.badRequest().build());
    }

    @ExceptionHandler(InvalidOfficeStateException.class)
    public Mono<ResponseEntity<Void>> handleInvalidOfficeState(InvalidOfficeStateException ex) {
        return Mono.just(ResponseEntity.badRequest().build());
    }

    @ExceptionHandler(InvalidStaffAssignmentScopeException.class)
    public Mono<ResponseEntity<Void>> handleInvalidStaffAssignmentScope(
            InvalidStaffAssignmentScopeException ex
    ) {
        return Mono.just(ResponseEntity.badRequest().build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return Mono.just(ResponseEntity.badRequest().build());
    }
}
