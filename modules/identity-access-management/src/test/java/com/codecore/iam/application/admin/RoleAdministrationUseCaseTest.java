package com.codecore.iam.application.admin;

import com.codecore.iam.application.command.CreateAdminRoleCommand;
import com.codecore.iam.application.command.UpdateAdminRoleCommand;
import com.codecore.iam.application.dto.AuthorizationContext;
import com.codecore.iam.application.port.out.AuthorizationContextAccessor;
import com.codecore.iam.application.port.out.MembershipRoleRepository;
import com.codecore.iam.application.port.out.RoleAdminQueryRepository;
import com.codecore.iam.application.port.out.RoleRepository;
import com.codecore.iam.domain.exception.RoleAlreadyExistsException;
import com.codecore.iam.domain.exception.RoleInUseException;
import com.codecore.iam.domain.exception.RoleNotFoundException;
import com.codecore.iam.domain.exception.SystemRoleImmutableException;
import com.codecore.iam.domain.model.role.Role;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.MembershipId;
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
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleAdministrationUseCaseTest {

    @Mock
    private AuthorizationContextAccessor authorizationContextAccessor;

    @Mock
    private RoleAdminQueryRepository roleAdminQueryRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private MembershipRoleRepository membershipRoleRepository;

    @Mock
    private TransactionalOperator transactionalOperator;

    private RoleAdministrationUseCaseImpl useCase;

    private final TenantId tenantId = TenantId.generate();
    private final RoleId roleId = RoleId.generate();
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
        useCase = new RoleAdministrationUseCaseImpl(
                authorizationContextAccessor,
                roleAdminQueryRepository,
                roleRepository,
                membershipRoleRepository,
                transactionalOperator
        );
    }

    @Test
    void shouldCreateCustomRole() {
        when(roleRepository.existsByTenantIdAndCode(tenantId, RoleCode.of("BILLING")))
                .thenReturn(Mono.just(false));
        when(roleRepository.save(any(Role.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        CreateAdminRoleCommand command = new CreateAdminRoleCommand("BILLING", "Billing Clerk");

        StepVerifier.create(useCase.execute(command))
                .assertNext(view -> {
                    assertThat(view.code()).isEqualTo("BILLING");
                    assertThat(view.systemRole()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    void shouldRejectDuplicateRoleCode() {
        when(roleRepository.existsByTenantIdAndCode(tenantId, RoleCode.of("BILLING")))
                .thenReturn(Mono.just(true));

        CreateAdminRoleCommand command = new CreateAdminRoleCommand("BILLING", "Billing Clerk");

        StepVerifier.create(useCase.execute(command))
                .expectError(RoleAlreadyExistsException.class)
                .verify();
    }

    @Test
    void shouldRejectSystemRoleUpdate() {
        Role systemRole = systemRole();
        when(roleRepository.findById(roleId)).thenReturn(Mono.just(systemRole));

        UpdateAdminRoleCommand command = new UpdateAdminRoleCommand(
                roleId,
                "New Name",
                null
        );

        StepVerifier.create(useCase.execute(command))
                .expectError(SystemRoleImmutableException.class)
                .verify();

        verify(roleRepository, never()).save(any());
    }

    @Test
    void shouldDeactivateCustomRoleOnUpdate() {
        Role customRole = customRole(RoleStatus.ACTIVE);
        when(roleRepository.findById(roleId)).thenReturn(Mono.just(customRole));
        when(roleRepository.save(any(Role.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        UpdateAdminRoleCommand command = new UpdateAdminRoleCommand(
                roleId,
                null,
                RoleStatus.INACTIVE
        );

        StepVerifier.create(useCase.execute(command))
                .assertNext(view -> assertThat(view.status()).isEqualTo(RoleStatus.INACTIVE))
                .verifyComplete();
    }

    @Test
    void shouldDeleteUnusedCustomRole() {
        Role customRole = customRole(RoleStatus.ACTIVE);
        when(roleRepository.findById(roleId)).thenReturn(Mono.just(customRole));
        when(membershipRoleRepository.existsByRoleId(roleId)).thenReturn(Mono.just(false));
        when(roleRepository.delete(roleId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.delete(roleId))
                .verifyComplete();

        verify(roleRepository).delete(roleId);
    }

    @Test
    void shouldRejectDeleteWhenRoleInUse() {
        Role customRole = customRole(RoleStatus.ACTIVE);
        when(roleRepository.findById(roleId)).thenReturn(Mono.just(customRole));
        when(membershipRoleRepository.existsByRoleId(roleId)).thenReturn(Mono.just(true));

        StepVerifier.create(useCase.delete(roleId))
                .expectError(RoleInUseException.class)
                .verify();

        verify(roleRepository, never()).delete(any());
    }

    @Test
    void shouldReturnNotFoundWhenRoleNotInTenant() {
        when(roleRepository.findById(roleId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.delete(roleId))
                .expectError(RoleNotFoundException.class)
                .verify();
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

    private Role customRole(RoleStatus status) {
        Instant now = Instant.now();
        return Role.reconstitute(
                roleId,
                tenantId,
                RoleCode.of("BILLING"),
                RoleName.of("Billing Clerk"),
                status,
                false,
                now,
                now,
                Set.of()
        );
    }
}
