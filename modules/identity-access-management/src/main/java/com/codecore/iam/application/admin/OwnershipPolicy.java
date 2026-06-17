package com.codecore.iam.application.admin;

import com.codecore.iam.application.dto.AuthorizationContext;
import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.application.port.out.MembershipRoleRepository;
import com.codecore.iam.application.port.out.RoleRepository;
import com.codecore.iam.domain.exception.IdentityNotFoundException;
import com.codecore.iam.domain.exception.OwnershipDeniedException;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.MembershipId;
import com.codecore.iam.domain.valueobject.TenantId;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Enforces PASO 15.0.1 ownership matrix for user mutations.
 */
public final class OwnershipPolicy {

    private static final String DENIED = "Insufficient ownership privilege to modify this user";

    private final MembershipRepository membershipRepository;
    private final MembershipRoleRepository membershipRoleRepository;
    private final RoleRepository roleRepository;

    public OwnershipPolicy(
            MembershipRepository membershipRepository,
            MembershipRoleRepository membershipRoleRepository,
            RoleRepository roleRepository
    ) {
        this.membershipRepository = Objects.requireNonNull(membershipRepository, "membershipRepository");
        this.membershipRoleRepository = Objects.requireNonNull(
                membershipRoleRepository,
                "membershipRoleRepository"
        );
        this.roleRepository = Objects.requireNonNull(roleRepository, "roleRepository");
    }

    public Mono<Void> assertCanModifyUser(AuthorizationContext actor, IdentityId targetIdentityId) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(targetIdentityId, "targetIdentityId");

        return resolvePrivilegeLevel(actor.membershipId(), actor.tenantId())
                .zipWith(resolveTargetPrivilegeLevel(targetIdentityId, actor.tenantId()))
                .flatMap(tuple -> {
                    int actorLevel = tuple.getT1();
                    int targetLevel = tuple.getT2();
                    if (canModify(actorLevel, targetLevel)) {
                        return Mono.empty();
                    }
                    return Mono.error(new OwnershipDeniedException(DENIED));
                });
    }

    private boolean canModify(int actorLevel, int targetLevel) {
        if (actorLevel >= RolePrivilegeLevel.OWNER.level()) {
            return true;
        }
        if (actorLevel >= RolePrivilegeLevel.ADMIN.level()) {
            return targetLevel <= RolePrivilegeLevel.MANAGER.level();
        }
        if (actorLevel >= RolePrivilegeLevel.MANAGER.level()) {
            return targetLevel <= RolePrivilegeLevel.USER.level();
        }
        return false;
    }

    private Mono<Integer> resolvePrivilegeLevel(MembershipId membershipId, TenantId tenantId) {
        return membershipRoleRepository.findByMembershipId(membershipId)
                .flatMap(assignment -> roleRepository.findById(assignment.roleId()))
                .filter(role -> role.tenantId().equals(tenantId))
                .map(role -> RolePrivilegeLevel.fromRoleCode(role.code()).level())
                .reduce(Integer::max)
                .defaultIfEmpty(RolePrivilegeLevel.USER.level());
    }

    private Mono<Integer> resolveTargetPrivilegeLevel(IdentityId targetIdentityId, TenantId tenantId) {
        return membershipRepository.findByIdentityIdAndTenantId(targetIdentityId, tenantId)
                .switchIfEmpty(Mono.error(new IdentityNotFoundException(
                        "User not found in tenant context")))
                .flatMap(membership -> resolvePrivilegeLevel(membership.id(), tenantId));
    }
}
