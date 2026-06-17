package com.codecore.iam.application.admin;

import com.codecore.iam.application.command.CreateAdminRoleCommand;
import com.codecore.iam.application.command.UpdateAdminRoleCommand;
import com.codecore.iam.application.dto.AdminRoleView;
import com.codecore.iam.application.dto.PagedResult;
import com.codecore.iam.application.port.in.CreateAdminRoleUseCase;
import com.codecore.iam.application.port.in.DeleteAdminRoleUseCase;
import com.codecore.iam.application.port.in.GetAdminRoleUseCase;
import com.codecore.iam.application.port.in.ListAdminRolesUseCase;
import com.codecore.iam.application.port.in.UpdateAdminRoleUseCase;
import com.codecore.iam.application.port.out.AuthorizationContextAccessor;
import com.codecore.iam.application.port.out.MembershipRoleRepository;
import com.codecore.iam.application.port.out.RoleAdminQueryRepository;
import com.codecore.iam.application.port.out.RoleRepository;
import com.codecore.iam.application.query.PageQuery;
import com.codecore.iam.domain.exception.InvalidDomainValueException;
import com.codecore.iam.domain.exception.RoleAlreadyExistsException;
import com.codecore.iam.domain.exception.RoleInUseException;
import com.codecore.iam.domain.exception.RoleNotFoundException;
import com.codecore.iam.domain.exception.SystemRoleImmutableException;
import com.codecore.iam.domain.model.role.Role;
import com.codecore.iam.domain.valueobject.RoleCode;
import com.codecore.iam.domain.valueobject.RoleId;
import com.codecore.iam.domain.valueobject.RoleName;
import com.codecore.iam.domain.valueobject.RoleStatus;
import com.codecore.iam.domain.valueobject.TenantId;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;

/**
 * Role administration use cases (FASE 15.3).
 */
public final class RoleAdministrationUseCaseImpl
        implements ListAdminRolesUseCase,
        GetAdminRoleUseCase,
        CreateAdminRoleUseCase,
        UpdateAdminRoleUseCase,
        DeleteAdminRoleUseCase {

    private final AuthorizationContextAccessor authorizationContextAccessor;
    private final RoleAdminQueryRepository roleAdminQueryRepository;
    private final RoleRepository roleRepository;
    private final MembershipRoleRepository membershipRoleRepository;
    private final TransactionalOperator transactionalOperator;

    public RoleAdministrationUseCaseImpl(
            AuthorizationContextAccessor authorizationContextAccessor,
            RoleAdminQueryRepository roleAdminQueryRepository,
            RoleRepository roleRepository,
            MembershipRoleRepository membershipRoleRepository,
            TransactionalOperator transactionalOperator
    ) {
        this.authorizationContextAccessor = Objects.requireNonNull(
                authorizationContextAccessor,
                "authorizationContextAccessor"
        );
        this.roleAdminQueryRepository = Objects.requireNonNull(roleAdminQueryRepository, "roleAdminQueryRepository");
        this.roleRepository = Objects.requireNonNull(roleRepository, "roleRepository");
        this.membershipRoleRepository = Objects.requireNonNull(
                membershipRoleRepository,
                "membershipRoleRepository"
        );
        this.transactionalOperator = Objects.requireNonNull(transactionalOperator, "transactionalOperator");
    }

    @Override
    public Mono<PagedResult<AdminRoleView>> execute(PageQuery pageQuery) {
        return authorizationContextAccessor.current()
                .flatMap(ctx -> roleAdminQueryRepository.countByTenantId(ctx.tenantId())
                        .flatMap(total -> roleAdminQueryRepository
                                .findByTenantId(ctx.tenantId(), pageQuery)
                                .collectList()
                                .map(content -> PagedResult.of(
                                        content,
                                        pageQuery.page(),
                                        pageQuery.size(),
                                        total
                                ))));
    }

    @Override
    public Mono<AdminRoleView> execute(RoleId roleId) {
        return authorizationContextAccessor.current()
                .flatMap(ctx -> loadRoleInTenant(ctx.tenantId(), roleId).map(this::toView));
    }

    @Override
    public Mono<AdminRoleView> execute(CreateAdminRoleCommand command) {
        return authorizationContextAccessor.current()
                .flatMap(ctx -> {
                    RoleCode code = RoleCode.of(command.code());
                    RoleName name = RoleName.of(command.name());
                    return roleRepository.existsByTenantIdAndCode(ctx.tenantId(), code)
                            .flatMap(exists -> {
                                if (exists) {
                                    return Mono.error(new RoleAlreadyExistsException(
                                            "Role code already exists in tenant"));
                                }
                                Instant now = Instant.now();
                                Role role = Role.create(ctx.tenantId(), code, name, now);
                                return roleRepository.save(role).map(this::toView);
                            });
                })
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<AdminRoleView> execute(UpdateAdminRoleCommand command) {
        return authorizationContextAccessor.current()
                .flatMap(ctx -> loadRoleInTenant(ctx.tenantId(), command.roleId())
                        .flatMap(role -> assertCustomRole(role)
                                .then(Mono.defer(() -> applyUpdates(role, command)))
                                .flatMap(roleRepository::save)
                                .map(this::toView)))
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<Void> delete(RoleId roleId) {
        return authorizationContextAccessor.current()
                .flatMap(ctx -> loadRoleInTenant(ctx.tenantId(), roleId)
                        .flatMap(role -> assertCustomRole(role)
                                .then(membershipRoleRepository.existsByRoleId(role.id()))
                                .flatMap(inUse -> {
                                    if (inUse) {
                                        return Mono.error(new RoleInUseException(
                                                "Role is assigned to memberships"));
                                    }
                                    return roleRepository.delete(role.id());
                                })))
                .as(transactionalOperator::transactional);
    }

    private Mono<Role> loadRoleInTenant(TenantId tenantId, RoleId roleId) {
        return roleRepository.findById(roleId)
                .filter(role -> role.tenantId().equals(tenantId))
                .switchIfEmpty(Mono.error(new RoleNotFoundException("Role not found in tenant context")));
    }

    private Mono<Void> assertCustomRole(Role role) {
        if (role.systemRole()) {
            return Mono.error(new SystemRoleImmutableException("System roles cannot be modified"));
        }
        return Mono.empty();
    }

    private Mono<Role> applyUpdates(Role role, UpdateAdminRoleCommand command) {
        boolean hasName = command.name() != null && !command.name().isBlank();
        boolean hasStatus = command.status() != null;
        if (!hasName && !hasStatus) {
            return Mono.error(new InvalidDomainValueException("name or status is required"));
        }
        if (hasName) {
            role.rename(RoleName.of(command.name()));
        }
        if (hasStatus) {
            if (command.status() == RoleStatus.ACTIVE) {
                role.activate();
            } else if (command.status() == RoleStatus.INACTIVE) {
                role.deactivate();
            } else {
                return Mono.error(new InvalidDomainValueException("Unsupported role status"));
            }
        }
        return Mono.just(role);
    }

    private AdminRoleView toView(Role role) {
        return new AdminRoleView(
                role.id(),
                role.tenantId(),
                role.code().value(),
                role.name().value(),
                role.status(),
                role.systemRole(),
                role.createdAt(),
                role.updatedAt()
        );
    }
}
