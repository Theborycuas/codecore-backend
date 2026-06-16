package com.codecore.iam.application;

import com.codecore.iam.application.command.CreateTenantCommand;
import com.codecore.iam.application.port.out.TenantRepository;
import com.codecore.iam.application.port.out.TenantSystemRolesProvisioner;
import com.codecore.iam.domain.exception.InvalidDomainValueException;
import com.codecore.iam.domain.exception.TenantAlreadyExistsException;
import com.codecore.iam.domain.model.tenant.Tenant;
import com.codecore.iam.domain.valueobject.TenantName;
import com.codecore.iam.domain.valueobject.TenantStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateTenantUseCaseTest {

    private static final String TENANT_NAME = "PetNova Demo";

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private TenantSystemRolesProvisioner tenantSystemRolesProvisioner;

    private CreateTenantUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateTenantUseCaseImpl(tenantRepository, tenantSystemRolesProvisioner);
    }

    @Test
    void shouldCreateTenantSuccessfully() {
        when(tenantRepository.existsByName(any(TenantName.class))).thenReturn(Mono.just(false));
        when(tenantRepository.save(any(Tenant.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(tenantSystemRolesProvisioner.provisionForTenant(any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(new CreateTenantCommand(TENANT_NAME)))
                .assertNext(result -> {
                    assertThat(result.tenantId()).isNotNull();
                    assertThat(result.name().value()).isEqualTo(TENANT_NAME);
                    assertThat(result.status()).isEqualTo(TenantStatus.ACTIVE);
                })
                .verifyComplete();

        ArgumentCaptor<Tenant> saved = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(saved.capture());
        assertThat(saved.getValue().name().value()).isEqualTo(TENANT_NAME);
        assertThat(saved.getValue().status()).isEqualTo(TenantStatus.ACTIVE);
    }

    @Test
    void shouldRejectDuplicateName() {
        when(tenantRepository.existsByName(eq(TenantName.of(TENANT_NAME)))).thenReturn(Mono.just(true));

        StepVerifier.create(useCase.execute(new CreateTenantCommand(TENANT_NAME)))
                .expectError(TenantAlreadyExistsException.class)
                .verify();

        verify(tenantRepository, never()).save(any());
    }

    @Test
    void shouldRejectBlankName() {
        StepVerifier.create(useCase.execute(new CreateTenantCommand("   ")))
                .expectError(InvalidDomainValueException.class)
                .verify();

        verify(tenantRepository, never()).existsByName(any());
    }

    @Test
    void shouldRejectInvalidNameLength() {
        StepVerifier.create(useCase.execute(new CreateTenantCommand("a".repeat(201))))
                .expectError(InvalidDomainValueException.class)
                .verify();

        verify(tenantRepository, never()).existsByName(any());
    }
}
