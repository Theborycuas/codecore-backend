package com.codecore.iam.application.admin;

import com.codecore.iam.application.authorization.IamPermissionCatalog;
import com.codecore.iam.application.command.ReplaceAdminRolePermissionsCommand;
import com.codecore.iam.application.dto.AdminRolePermissionView;
import com.codecore.iam.application.dto.AuthorizationContext;
import com.codecore.iam.application.port.out.AuthorizationContextAccessor;
import com.codecore.iam.application.port.out.PermissionRepository;
import com.codecore.iam.application.port.out.RolePermissionAdminQueryRepository;
import com.codecore.iam.application.port.out.RolePermissionRepository;
import com.codecore.iam.application.port.out.RoleRepository;
import com.codecore.iam.domain.exception.SystemRoleImmutableException;
import com.codecore.iam.domain.model.permission.Permission;
import com.codecore.iam.domain.model.role.Role;
import com.codecore.iam.domain.model.role.RolePermissionAssignment;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.MembershipId;
import com.codecore.iam.domain.valueobject.PermissionId;
import com.codecore.iam.domain.valueobject.RoleCode;
import com.codecore.iam.domain.valueobject.RoleId;
import com.codecore.iam.domain.valueobject.RoleName;
import com.codecore.iam.domain.valueobject.RoleStatus;
import com.codecore.iam.domain.valueobject.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RolePermissionAdministrationUseCaseTest {

    @Mock
    private AuthorizationContextAccessor authorizationContextAccessor;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private RolePermissionAdminQueryRepository rolePermissionAdminQueryRepository;

    @Mock
    private TransactionalOperator transactionalOperator;

    private RolePermissionAdministrationUseCaseImpl useCase;

    private final TenantId tenantId = TenantId.generate();
    private final RoleId roleId = RoleId.generate();
    private final PermissionId readPermissionId = PermissionId.generate();
    private final PermissionId updatePermissionId = PermissionId.generate();
    private final AuthorizationContext context = new AuthorizationContext(
            IdentityId.generate(),
            tenantId,
            MembershipId.generate()
    );

    @BeforeEach
    void setUp() {
        lenient().when(authorizationContextAccessor.current()).thenReturn(Mono.just(context));
        lenient().when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        useCase = new RolePermissionAdministrationUseCaseImpl(
                authorizationContextAccessor,
                roleRepository,
                permissionRepository,
                rolePermissionRepository,
                rolePermissionAdminQueryRepository,
                transactionalOperator
        );
    }

    @Test
    void shouldListRolePermissions() {
        Role customRole = customRole();
        when(roleRepository.findById(roleId)).thenReturn(Mono.just(customRole));
        when(rolePermissionAdminQueryRepository.findByRoleId(roleId))
                .thenReturn(Flux.just(new AdminRolePermissionView(
                        readPermissionId,
                        "user:read",
                        "Read users",
                        Instant.now()
                )));

        StepVerifier.create(useCase.execute(roleId))
                .assertNext(views -> assertThat(views).hasSize(1))
                .verifyComplete();
    }

    @Test
    void shouldReplaceRolePermissions() {
        Role customRole = customRole();
        Permission readPermission = permission(readPermissionId, IamPermissionCatalog.USER_READ);
        Permission updatePermission = permission(updatePermissionId, IamPermissionCatalog.USER_UPDATE);

        when(roleRepository.findById(roleId)).thenReturn(Mono.just(customRole));
        when(permissionRepository.findById(readPermissionId)).thenReturn(Mono.just(readPermission));
        when(permissionRepository.findById(updatePermissionId)).thenReturn(Mono.just(updatePermission));
        when(rolePermissionRepository.findByRoleId(roleId))
                .thenReturn(Flux.just(RolePermissionAssignment.assign(readPermissionId, Instant.now())));
        when(rolePermissionRepository.replaceAll(eq(roleId), any())).thenReturn(Mono.empty());
        when(rolePermissionAdminQueryRepository.findByRoleId(roleId))
                .thenReturn(Flux.just(
                        new AdminRolePermissionView(
                                readPermissionId,
                                "user:read",
                                "Read users",
                                Instant.now()
                        ),
                        new AdminRolePermissionView(
                                updatePermissionId,
                                "user:update",
                                "Update users",
                                Instant.now()
                        )
                ));

        ReplaceAdminRolePermissionsCommand command = new ReplaceAdminRolePermissionsCommand(
                roleId,
                List.of(readPermissionId.value(), updatePermissionId.value())
        );

        StepVerifier.create(useCase.execute(command))
                .assertNext(views -> assertThat(views).hasSize(2))
                .verifyComplete();

        verify(rolePermissionRepository).replaceAll(eq(roleId), any());
    }

    @Test
    void shouldRejectSystemRolePermissionReplace() {
        when(roleRepository.findById(roleId)).thenReturn(Mono.just(systemRole()));

        ReplaceAdminRolePermissionsCommand command = new ReplaceAdminRolePermissionsCommand(
                roleId,
                List.of(readPermissionId.value())
        );

        StepVerifier.create(useCase.execute(command))
                .expectError(SystemRoleImmutableException.class)
                .verify();

        verify(rolePermissionRepository, never()).replaceAll(any(), any());
    }

    private Role customRole() {
        Instant now = Instant.now();
        return Role.reconstitute(
                roleId,
                tenantId,
                RoleCode.of("CUSTOM"),
                RoleName.of("Custom"),
                RoleStatus.ACTIVE,
                false,
                now,
                now,
                Set.of()
        );
    }

    private Role systemRole() {
        Instant now = Instant.now();
        return Role.reconstitute(
                roleId,
                tenantId,
                RoleCode.of("ADMIN"),
                RoleName.of("Administrator"),
                RoleStatus.ACTIVE,
                true,
                now,
                now,
                Set.of()
        );
    }

    private Permission permission(PermissionId id, com.codecore.iam.domain.valueobject.PermissionCode code) {
        Instant now = Instant.now();
        return new Permission(
                id,
                code,
                code.value(),
                true,
                now,
                now
        );
    }
}
