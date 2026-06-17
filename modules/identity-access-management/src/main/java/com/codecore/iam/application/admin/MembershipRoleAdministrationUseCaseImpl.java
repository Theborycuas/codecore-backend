package com.codecore.iam.application.admin;

import com.codecore.iam.application.command.ReplaceAdminMembershipRolesCommand;
import com.codecore.iam.application.dto.AdminMembershipRoleView;
import com.codecore.iam.application.port.in.GetAdminMembershipRolesUseCase;
import com.codecore.iam.application.port.in.ReplaceAdminMembershipRolesUseCase;
import com.codecore.iam.application.port.out.AuthorizationContextAccessor;
import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.application.port.out.MembershipRoleAdminQueryRepository;
import com.codecore.iam.application.port.out.MembershipRoleRepository;
import com.codecore.iam.application.port.out.RoleRepository;
import com.codecore.iam.domain.exception.InvalidDomainValueException;
import com.codecore.iam.domain.exception.MembershipNotFoundException;
import com.codecore.iam.domain.exception.RoleNotFoundException;
import com.codecore.iam.domain.model.membership.IdentityTenantMembership;
import com.codecore.iam.domain.model.membership.MembershipRoleAssignment;
import com.codecore.iam.domain.valueobject.MembershipId;
import com.codecore.iam.domain.valueobject.MembershipStatus;
import com.codecore.iam.domain.valueobject.RoleId;
import com.codecore.iam.domain.valueobject.TenantId;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Membership ↔ Role administration (FASE 15.6).
 */
public final class MembershipRoleAdministrationUseCaseImpl
        implements GetAdminMembershipRolesUseCase,
        ReplaceAdminMembershipRolesUseCase {

    private final AuthorizationContextAccessor authorizationContextAccessor;
    private final MembershipRepository membershipRepository;
    private final RoleRepository roleRepository;
    private final MembershipRoleRepository membershipRoleRepository;
    private final MembershipRoleAdminQueryRepository membershipRoleAdminQueryRepository;
    private final OwnershipPolicy ownershipPolicy;
    private final TransactionalOperator transactionalOperator;

    public MembershipRoleAdministrationUseCaseImpl(
            AuthorizationContextAccessor authorizationContextAccessor,
            MembershipRepository membershipRepository,
            RoleRepository roleRepository,
            MembershipRoleRepository membershipRoleRepository,
            MembershipRoleAdminQueryRepository membershipRoleAdminQueryRepository,
            OwnershipPolicy ownershipPolicy,
            TransactionalOperator transactionalOperator
    ) {
        this.authorizationContextAccessor = Objects.requireNonNull(
                authorizationContextAccessor,
                "authorizationContextAccessor"
        );
        this.membershipRepository = Objects.requireNonNull(membershipRepository, "membershipRepository");
        this.roleRepository = Objects.requireNonNull(roleRepository, "roleRepository");
        this.membershipRoleRepository = Objects.requireNonNull(
                membershipRoleRepository,
                "membershipRoleRepository"
        );
        this.membershipRoleAdminQueryRepository = Objects.requireNonNull(
                membershipRoleAdminQueryRepository,
                "membershipRoleAdminQueryRepository"
        );
        this.ownershipPolicy = Objects.requireNonNull(ownershipPolicy, "ownershipPolicy");
        this.transactionalOperator = Objects.requireNonNull(transactionalOperator, "transactionalOperator");
    }

    @Override
    public Mono<List<AdminMembershipRoleView>> execute(MembershipId membershipId) {
        return authorizationContextAccessor.current()
                .flatMap(ctx -> loadMembershipInTenant(ctx.tenantId(), membershipId)
                        .flatMap(membership -> membershipRoleAdminQueryRepository
                                .findByMembershipId(membership.id(), ctx.tenantId())
                                .collectList()));
    }

    @Override
    public Mono<List<AdminMembershipRoleView>> execute(ReplaceAdminMembershipRolesCommand command) {
        return authorizationContextAccessor.current()
                .flatMap(ctx -> loadMembershipInTenant(ctx.tenantId(), command.membershipId())
                .flatMap(membership -> assertMembershipActive(membership)
                        .then(Mono.defer(() -> ownershipPolicy.assertCanModifyUser(
                                ctx,
                                membership.identityId()
                        )))
                        .then(Mono.defer(() -> replaceRoles(
                                        ctx.tenantId(),
                                        membership,
                                        command.roleIds()
                                )))))
                .as(transactionalOperator::transactional);
    }

    private Mono<List<AdminMembershipRoleView>> replaceRoles(
            TenantId tenantId,
            IdentityTenantMembership membership,
            List<java.util.UUID> roleIdValues
    ) {
        List<RoleId> desiredRoleIds = normalizeRoleIds(roleIdValues);
        Instant now = Instant.now();

        return validateRolesInTenant(tenantId, desiredRoleIds)
                .then(membershipRoleRepository.findByMembershipId(membership.id()).collectList())
                .flatMap(currentAssignments -> {
                    IdentityTenantMembership mutableMembership = reconstituteWithAssignments(
                            membership,
                            currentAssignments
                    );
                    Set<RoleId> currentRoleIds = mutableMembership.assignedRoleIds();
                    Set<RoleId> desiredSet = new LinkedHashSet<>(desiredRoleIds);

                    for (RoleId roleId : desiredSet) {
                        if (!currentRoleIds.contains(roleId)) {
                            mutableMembership.assignRole(roleId, tenantId, now);
                        }
                    }
                    for (RoleId roleId : currentRoleIds) {
                        if (!desiredSet.contains(roleId)) {
                            mutableMembership.revokeRole(roleId);
                        }
                    }

                    return membershipRoleRepository
                            .replaceAll(membership.id(), mutableMembership.roleAssignments())
                            .then(membershipRoleAdminQueryRepository
                                    .findByMembershipId(membership.id(), tenantId)
                                    .collectList());
                });
    }

    private Mono<Void> validateRolesInTenant(TenantId tenantId, List<RoleId> roleIds) {
        return Mono.defer(() -> {
            Mono<Void> chain = Mono.empty();
            for (RoleId roleId : roleIds) {
                chain = chain.then(
                        Mono.defer(() -> roleRepository.findById(roleId)
                                .filter(role -> role.tenantId().equals(tenantId))
                                .switchIfEmpty(Mono.error(new RoleNotFoundException(
                                        "Role not found in tenant context")))
                                .then())
                );
            }
            return chain;
        });
    }

    private static List<RoleId> normalizeRoleIds(List<java.util.UUID> roleIdValues) {
        if (roleIdValues == null) {
            throw new InvalidDomainValueException("roleIds is required");
        }
        Set<java.util.UUID> unique = new HashSet<>();
        List<RoleId> normalized = new ArrayList<>();
        for (java.util.UUID value : roleIdValues) {
            if (value == null) {
                throw new InvalidDomainValueException("roleIds must not contain null");
            }
            if (!unique.add(value)) {
                throw new InvalidDomainValueException("roleIds must be unique");
            }
            normalized.add(new RoleId(value));
        }
        return normalized;
    }

    private static IdentityTenantMembership reconstituteWithAssignments(
            IdentityTenantMembership membership,
            List<MembershipRoleAssignment> assignments
    ) {
        return IdentityTenantMembership.reconstitute(
                membership.id(),
                membership.identityId(),
                membership.tenantId(),
                membership.status(),
                membership.createdAt(),
                membership.updatedAt(),
                new LinkedHashSet<>(assignments)
        );
    }

    private Mono<IdentityTenantMembership> loadMembershipInTenant(
            TenantId tenantId,
            MembershipId membershipId
    ) {
        return membershipRepository.findByIdAndTenantId(membershipId, tenantId)
                .switchIfEmpty(Mono.error(new MembershipNotFoundException(
                        "Membership not found in tenant context")));
    }

    private Mono<Void> assertMembershipActive(IdentityTenantMembership membership) {
        if (membership.status() != MembershipStatus.ACTIVE) {
            return Mono.error(new InvalidDomainValueException("Membership is not active"));
        }
        return Mono.empty();
    }
}
