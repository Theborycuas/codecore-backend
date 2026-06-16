package com.codecore.iam.application;

import com.codecore.iam.application.dto.AuthorizationContext;
import com.codecore.iam.application.port.out.AuthorizationQueryRepository;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.MembershipId;
import com.codecore.iam.domain.valueobject.PermissionCode;
import com.codecore.iam.domain.valueobject.RoleCode;
import com.codecore.iam.domain.valueobject.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private AuthorizationQueryRepository authorizationQueryRepository;

    private AuthorizationServiceImpl authorizationService;

    private AuthorizationContext context;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationServiceImpl(authorizationQueryRepository);
        context = new AuthorizationContext(
                IdentityId.generate(),
                TenantId.generate(),
                MembershipId.generate()
        );
    }

    @Test
    void shouldDelegateHasPermissionToQueryRepository() {
        PermissionCode permissionCode = PermissionCode.of("user:read");
        when(authorizationQueryRepository.existsPermissionForMembership(context.membershipId(), permissionCode))
                .thenReturn(Mono.just(true));

        StepVerifier.create(authorizationService.hasPermission(context, permissionCode))
                .expectNext(true)
                .verifyComplete();

        verify(authorizationQueryRepository).existsPermissionForMembership(context.membershipId(), permissionCode);
    }

    @Test
    void shouldReturnFalseWhenHasAnyPermissionReceivesNoCodes() {
        StepVerifier.create(authorizationService.hasAnyPermission(context))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldDelegateHasAnyPermissionToQueryRepository() {
        PermissionCode read = PermissionCode.of("user:read");
        PermissionCode write = PermissionCode.of("user:write");
        when(authorizationQueryRepository.existsAnyPermissionForMembership(
                eq(context.membershipId()),
                eq(List.of("user:read", "user:write"))
        )).thenReturn(Mono.just(true));

        StepVerifier.create(authorizationService.hasAnyPermission(context, read, write))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldDelegateHasRoleToQueryRepository() {
        RoleCode roleCode = RoleCode.of("ADMIN");
        when(authorizationQueryRepository.existsRoleForMembership(context.membershipId(), roleCode))
                .thenReturn(Mono.just(false));

        StepVerifier.create(authorizationService.hasRole(context, roleCode))
                .expectNext(false)
                .verifyComplete();
    }
}
