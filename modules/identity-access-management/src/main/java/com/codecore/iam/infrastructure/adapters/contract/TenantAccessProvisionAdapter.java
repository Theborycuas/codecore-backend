package com.codecore.iam.infrastructure.adapters.contract;

import com.codecore.iam.application.admin.IdentityRegistrationOrchestrator;
import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.application.port.out.MembershipRoleRepository;
import com.codecore.iam.application.port.out.RoleRepository;
import com.codecore.iam.contract.provision.TenantAccessProvisionPort;
import com.codecore.iam.domain.exception.InvalidDomainValueException;
import com.codecore.iam.domain.exception.MembershipAlreadyExistsException;
import com.codecore.iam.domain.exception.MembershipNotFoundException;
import com.codecore.iam.domain.exception.RoleNotFoundException;
import com.codecore.iam.domain.model.identity.Identity;
import com.codecore.iam.domain.model.membership.IdentityTenantMembership;
import com.codecore.iam.domain.model.membership.MembershipRoleAssignment;
import com.codecore.iam.domain.model.role.Role;
import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.IdentityStatus;
import com.codecore.iam.domain.valueobject.MembershipId;
import com.codecore.iam.domain.valueobject.RawPassword;
import com.codecore.iam.domain.valueobject.RoleCode;
import com.codecore.iam.domain.valueobject.TenantId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Provisions tenant access for Invitation / Access consumers (FASE 23).
 */
@Component
public class TenantAccessProvisionAdapter implements TenantAccessProvisionPort {

    private static final Set<String> ALLOWED_ROLE_CODES = Set.of(
            "ADMIN",
            "MANAGER",
            "USER",
            "READ_ONLY"
    );

    private final IdentityRepository identityRepository;
    private final MembershipRepository membershipRepository;
    private final MembershipRoleRepository membershipRoleRepository;
    private final RoleRepository roleRepository;
    private final IdentityRegistrationOrchestrator identityRegistrationOrchestrator;
    private final TransactionalOperator transactionalOperator;

    public TenantAccessProvisionAdapter(
            IdentityRepository identityRepository,
            MembershipRepository membershipRepository,
            MembershipRoleRepository membershipRoleRepository,
            RoleRepository roleRepository,
            IdentityRegistrationOrchestrator identityRegistrationOrchestrator,
            TransactionalOperator transactionalOperator
    ) {
        this.identityRepository = Objects.requireNonNull(identityRepository, "identityRepository");
        this.membershipRepository = Objects.requireNonNull(membershipRepository, "membershipRepository");
        this.membershipRoleRepository = Objects.requireNonNull(
                membershipRoleRepository,
                "membershipRoleRepository"
        );
        this.roleRepository = Objects.requireNonNull(roleRepository, "roleRepository");
        this.identityRegistrationOrchestrator = Objects.requireNonNull(
                identityRegistrationOrchestrator,
                "identityRegistrationOrchestrator"
        );
        this.transactionalOperator = Objects.requireNonNull(transactionalOperator, "transactionalOperator");
    }

    @Override
    public Mono<MembershipId> provision(ProvisionTenantAccessCommand cmd) {
        Objects.requireNonNull(cmd, "cmd");
        Objects.requireNonNull(cmd.tenantId(), "tenantId");
        Objects.requireNonNull(cmd.email(), "email");
        Objects.requireNonNull(cmd.now(), "now");

        String normalizedRole = validateAndNormalizeRoleCode(cmd.roleCode());
        TenantId tenantId = cmd.tenantId();
        EmailAddress email = cmd.email();
        Instant now = cmd.now();

        Mono<MembershipId> flow = ensureNoActiveMembership(email, tenantId)
                .then(identityRepository.findByEmail(email)
                        .flatMap(identity -> linkExistingIdentity(tenantId, identity, now))
                        .switchIfEmpty(Mono.defer(() -> registerNewIdentity(tenantId, email, cmd.passwordOrNull())))
                        .flatMap(membership -> assignSystemRole(membership, tenantId, normalizedRole, now)));

        return flow.as(transactionalOperator::transactional);
    }

    private Mono<Void> ensureNoActiveMembership(EmailAddress email, TenantId tenantId) {
        return identityRepository.findByEmail(email)
                .flatMap(identity -> membershipRepository
                        .findActiveByIdentityIdAndTenantId(identity.id(), tenantId)
                        .flatMap(existing -> Mono.<Void>error(new MembershipAlreadyExistsException(
                                "Active membership already exists for email in tenant")))
                        .then())
                .switchIfEmpty(Mono.empty());
    }

    private Mono<IdentityTenantMembership> linkExistingIdentity(
            TenantId tenantId,
            Identity identity,
            Instant now
    ) {
        return membershipRepository.exists(identity.id(), tenantId)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new MembershipAlreadyExistsException(
                                "Membership already exists for identity in tenant"));
                    }
                    IdentityTenantMembership membership = IdentityTenantMembership.create(
                            identity.id(),
                            tenantId,
                            now
                    );
                    return membershipRepository.save(membership);
                });
    }

    private Mono<IdentityTenantMembership> registerNewIdentity(
            TenantId tenantId,
            EmailAddress email,
            RawPassword passwordOrNull
    ) {
        if (passwordOrNull == null) {
            return Mono.error(new InvalidDomainValueException(
                    "Password is required when identity does not exist"));
        }
        return identityRegistrationOrchestrator.registerNewIdentity(
                        tenantId,
                        email,
                        passwordOrNull,
                        IdentityStatus.ACTIVE
                )
                .flatMap(identity -> membershipRepository.findByIdentityIdAndTenantId(identity.id(), tenantId))
                .switchIfEmpty(Mono.error(new MembershipNotFoundException(
                        "Membership was not created for new identity")));
    }

    private Mono<MembershipId> assignSystemRole(
            IdentityTenantMembership membership,
            TenantId tenantId,
            String roleCode,
            Instant now
    ) {
        return roleRepository.findByTenantIdAndCode(tenantId, RoleCode.of(roleCode))
                .switchIfEmpty(Mono.error(new RoleNotFoundException(
                        "System role not found for code: " + roleCode)))
                .flatMap(role -> requireSystemRole(role)
                        .then(membershipRoleRepository.assign(
                                membership.id(),
                                MembershipRoleAssignment.assign(role.id(), now)
                        ))
                        .thenReturn(membership.id()));
    }

    private static Mono<Role> requireSystemRole(Role role) {
        if (!role.systemRole()) {
            return Mono.error(new InvalidDomainValueException(
                    "Role is not a system role: " + role.code().value()));
        }
        return Mono.just(role);
    }

    private static String validateAndNormalizeRoleCode(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            throw new InvalidDomainValueException("roleCode is required");
        }
        String normalized = roleCode.trim().toUpperCase(Locale.ROOT);
        if ("OWNER".equals(normalized)) {
            throw new InvalidDomainValueException("OWNER role cannot be assigned via tenant access provision");
        }
        if (!ALLOWED_ROLE_CODES.contains(normalized)) {
            throw new InvalidDomainValueException(
                    "roleCode must be one of ADMIN, MANAGER, USER, READ_ONLY");
        }
        return normalized;
    }
}
