package com.codecore.iam.application;

import com.codecore.iam.application.admin.IdentityRegistrationOrchestrator;
import com.codecore.iam.application.authorization.SystemRoleTemplate;
import com.codecore.iam.application.command.CreateTenantCommand;
import com.codecore.iam.application.dto.BootstrapPlatformResult;
import com.codecore.iam.application.dto.CreateTenantResponse;
import com.codecore.iam.application.port.in.CreateTenantUseCase;
import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.application.port.out.MembershipRoleRepository;
import com.codecore.iam.application.port.out.RoleRepository;
import com.codecore.iam.application.port.out.TenantRepository;
import com.codecore.iam.configuration.PlatformBootstrapProperties;
import com.codecore.iam.domain.model.identity.Identity;
import com.codecore.iam.domain.model.membership.IdentityTenantMembership;
import com.codecore.iam.domain.model.role.Role;
import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.IdentityStatus;
import com.codecore.iam.domain.valueobject.RawPassword;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.domain.valueobject.TenantName;
import com.codecore.iam.domain.valueobject.TenantStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BootstrapPlatformUseCaseTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private CreateTenantUseCase createTenantUseCase;

    @Mock
    private IdentityRegistrationOrchestrator identityRegistrationOrchestrator;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private MembershipRoleRepository membershipRoleRepository;

    private PlatformBootstrapProperties properties;
    private BootstrapPlatformUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        properties = new PlatformBootstrapProperties();
        useCase = new BootstrapPlatformUseCaseImpl(
                properties,
                tenantRepository,
                createTenantUseCase,
                identityRegistrationOrchestrator,
                membershipRepository,
                roleRepository,
                membershipRoleRepository
        );
    }

    @Test
    void shouldSkipWhenDisabled() {
        properties.setEnabled(false);

        StepVerifier.create(useCase.executeIfNeeded())
                .assertNext(result -> assertThat(result.executed()).isFalse())
                .verifyComplete();

        verify(tenantRepository, never()).count();
    }

    @Test
    void shouldSkipWhenTenantsAlreadyExist() {
        properties.setEnabled(true);
        when(tenantRepository.count()).thenReturn(Mono.just(1L));

        StepVerifier.create(useCase.executeIfNeeded())
                .assertNext(result -> assertThat(result.executed()).isFalse())
                .verifyComplete();
    }

    @Test
    void shouldBootstrapTenantOwnerAndMembership() {
        properties.setEnabled(true);
        properties.setTenantName("Acme");
        properties.setOwnerEmail("owner@acme.local");
        properties.setOwnerPassword("ValidPass1!");

        TenantId tenantId = TenantId.generate();
        IdentityId identityId = IdentityId.generate();
        Identity identity = new Identity(
                identityId,
                tenantId,
                EmailAddress.of("owner@acme.local"),
                IdentityStatus.ACTIVE,
                null,
                null,
                Instant.now(),
                Instant.now(),
                0L
        );
        IdentityTenantMembership membership = IdentityTenantMembership.create(identityId, tenantId, Instant.now());
        Role ownerRole = Role.createSystemRole(
                tenantId,
                SystemRoleTemplate.OWNER.code(),
                SystemRoleTemplate.OWNER.roleName(),
                Instant.now()
        );

        when(tenantRepository.count()).thenReturn(Mono.just(0L));
        when(createTenantUseCase.execute(new CreateTenantCommand("Acme")))
                .thenReturn(Mono.just(new CreateTenantResponse(
                        tenantId,
                        TenantName.of("Acme"),
                        TenantStatus.ACTIVE
                )));
        when(identityRegistrationOrchestrator.registerNewIdentity(
                eq(tenantId),
                any(EmailAddress.class),
                any(RawPassword.class),
                eq(IdentityStatus.ACTIVE)
        )).thenReturn(Mono.just(identity));
        when(membershipRepository.findByIdentityIdAndTenantId(identityId, tenantId))
                .thenReturn(Mono.just(membership));
        when(roleRepository.findByTenantIdAndCode(tenantId, SystemRoleTemplate.OWNER.code()))
                .thenReturn(Mono.just(ownerRole));
        when(membershipRoleRepository.assign(any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.executeIfNeeded())
                .assertNext(result -> {
                    assertThat(result.executed()).isTrue();
                    assertThat(result.tenantId()).isEqualTo(tenantId);
                    assertThat(result.ownerEmail()).isEqualTo("owner@acme.local");
                })
                .verifyComplete();
    }
}
