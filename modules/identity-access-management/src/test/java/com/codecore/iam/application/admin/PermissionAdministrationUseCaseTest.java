package com.codecore.iam.application.admin;

import com.codecore.iam.application.authorization.IamPermissionCatalog;
import com.codecore.iam.application.dto.AuthorizationContext;
import com.codecore.iam.application.port.out.AuthorizationContextAccessor;
import com.codecore.iam.application.port.out.PermissionAdminQueryRepository;
import com.codecore.iam.application.port.out.PermissionRepository;
import com.codecore.iam.application.query.PageQuery;
import com.codecore.iam.domain.exception.PermissionNotFoundException;
import com.codecore.iam.domain.model.permission.Permission;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.MembershipId;
import com.codecore.iam.domain.valueobject.PermissionCode;
import com.codecore.iam.domain.valueobject.PermissionId;
import com.codecore.iam.domain.valueobject.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionAdministrationUseCaseTest {

    @Mock
    private AuthorizationContextAccessor authorizationContextAccessor;

    @Mock
    private PermissionAdminQueryRepository permissionAdminQueryRepository;

    @Mock
    private PermissionRepository permissionRepository;

    private PermissionAdministrationUseCaseImpl useCase;

    private final TenantId tenantId = TenantId.generate();
    private final PermissionId permissionId = PermissionId.generate();
    private final AuthorizationContext context = new AuthorizationContext(
            IdentityId.generate(),
            tenantId,
            MembershipId.generate()
    );

    @BeforeEach
    void setUp() {
        when(authorizationContextAccessor.current()).thenReturn(Mono.just(context));
        useCase = new PermissionAdministrationUseCaseImpl(
                authorizationContextAccessor,
                permissionAdminQueryRepository,
                permissionRepository
        );
    }

    @Test
    void shouldListPermissions() {
        PageQuery pageQuery = new PageQuery(0, 20, "code", PageQuery.SortDirection.ASC);
        var view = new com.codecore.iam.application.dto.AdminPermissionView(
                permissionId,
                "user:read",
                "Read users",
                true,
                Instant.now(),
                Instant.now()
        );
        when(permissionAdminQueryRepository.countAll()).thenReturn(Mono.just(1L));
        when(permissionAdminQueryRepository.findAll(pageQuery)).thenReturn(Flux.just(view));

        StepVerifier.create(useCase.execute(pageQuery))
                .assertNext(result -> {
                    assertThat(result.content()).hasSize(1);
                    assertThat(result.totalElements()).isEqualTo(1L);
                })
                .verifyComplete();
    }

    @Test
    void shouldGetPermissionById() {
        Permission permission = permission();
        when(permissionRepository.findById(permissionId)).thenReturn(Mono.just(permission));

        StepVerifier.create(useCase.execute(permissionId))
                .assertNext(view -> assertThat(view.code()).isEqualTo("user:read"))
                .verifyComplete();
    }

    @Test
    void shouldReturnNotFoundWhenPermissionMissing() {
        when(permissionRepository.findById(permissionId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(permissionId))
                .expectError(PermissionNotFoundException.class)
                .verify();
    }

    private Permission permission() {
        Instant now = Instant.now();
        return Permission.createSystemPermission(
                IamPermissionCatalog.USER_READ,
                "Read users",
                now
        );
    }
}
