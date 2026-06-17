package com.codecore.iam.application.admin;

import com.codecore.iam.application.dto.AdminPermissionView;
import com.codecore.iam.application.dto.PagedResult;
import com.codecore.iam.application.port.in.GetAdminPermissionUseCase;
import com.codecore.iam.application.port.in.ListAdminPermissionsUseCase;
import com.codecore.iam.application.port.out.AuthorizationContextAccessor;
import com.codecore.iam.application.port.out.PermissionAdminQueryRepository;
import com.codecore.iam.application.port.out.PermissionRepository;
import com.codecore.iam.application.query.PageQuery;
import com.codecore.iam.domain.exception.PermissionNotFoundException;
import com.codecore.iam.domain.model.permission.Permission;
import com.codecore.iam.domain.valueobject.PermissionId;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Permission catalog administration (FASE 15.4) — read-only global catalog.
 */
public final class PermissionAdministrationUseCaseImpl
        implements ListAdminPermissionsUseCase,
        GetAdminPermissionUseCase {

    private final AuthorizationContextAccessor authorizationContextAccessor;
    private final PermissionAdminQueryRepository permissionAdminQueryRepository;
    private final PermissionRepository permissionRepository;

    public PermissionAdministrationUseCaseImpl(
            AuthorizationContextAccessor authorizationContextAccessor,
            PermissionAdminQueryRepository permissionAdminQueryRepository,
            PermissionRepository permissionRepository
    ) {
        this.authorizationContextAccessor = Objects.requireNonNull(
                authorizationContextAccessor,
                "authorizationContextAccessor"
        );
        this.permissionAdminQueryRepository = Objects.requireNonNull(
                permissionAdminQueryRepository,
                "permissionAdminQueryRepository"
        );
        this.permissionRepository = Objects.requireNonNull(permissionRepository, "permissionRepository");
    }

    @Override
    public Mono<PagedResult<AdminPermissionView>> execute(PageQuery pageQuery) {
        return authorizationContextAccessor.current()
                .flatMap(ctx -> permissionAdminQueryRepository.countAll()
                        .flatMap(total -> permissionAdminQueryRepository
                                .findAll(pageQuery)
                                .collectList()
                                .map(content -> PagedResult.of(
                                        content,
                                        pageQuery.page(),
                                        pageQuery.size(),
                                        total
                                ))));
    }

    @Override
    public Mono<AdminPermissionView> execute(PermissionId permissionId) {
        return authorizationContextAccessor.current()
                .flatMap(ctx -> permissionRepository.findById(permissionId)
                        .map(this::toView)
                        .switchIfEmpty(Mono.error(new PermissionNotFoundException(
                                "Permission not found"))));
    }

    private AdminPermissionView toView(Permission permission) {
        return new AdminPermissionView(
                permission.id(),
                permission.code().value(),
                permission.description(),
                permission.systemPermission(),
                permission.createdAt(),
                permission.updatedAt()
        );
    }
}
