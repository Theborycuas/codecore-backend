package com.codecore.iam.application.admin;

import com.codecore.iam.application.command.UpdateAdminTenantCommand;
import com.codecore.iam.application.dto.AuthorizationContext;
import com.codecore.iam.application.port.out.AuthorizationContextAccessor;
import com.codecore.iam.application.port.out.TenantRepository;
import com.codecore.iam.domain.exception.InvalidDomainValueException;
import com.codecore.iam.domain.exception.TenantAlreadyExistsException;
import com.codecore.iam.domain.model.tenant.Tenant;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.MembershipId;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.domain.valueobject.TenantName;
import com.codecore.iam.domain.valueobject.TenantStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantAdministrationUseCaseTest {

    @Mock
    private AuthorizationContextAccessor authorizationContextAccessor;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private TransactionalOperator transactionalOperator;

    private TenantAdministrationUseCaseImpl useCase;

    private final TenantId tenantId = TenantId.generate();
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
        useCase = new TenantAdministrationUseCaseImpl(
                authorizationContextAccessor,
                tenantRepository,
                transactionalOperator
        );
    }

    @Test
    void shouldGetCurrentTenant() {
        Instant now = Instant.parse("2026-06-01T12:00:00Z");
        Tenant tenant = Tenant.create(tenantId, TenantName.of("Acme"), now);
        when(tenantRepository.findById(tenantId)).thenReturn(Mono.just(tenant));

        StepVerifier.create(useCase.execute())
                .assertNext(view -> {
                    assertThat(view.id()).isEqualTo(tenantId);
                    assertThat(view.name()).isEqualTo("Acme");
                    assertThat(view.status()).isEqualTo(TenantStatus.ACTIVE);
                })
                .verifyComplete();
    }

    @Test
    void shouldUpdateTenantNameAndStatus() {
        Instant now = Instant.parse("2026-06-01T12:00:00Z");
        Tenant tenant = Tenant.create(tenantId, TenantName.of("Acme"), now);
        when(tenantRepository.findById(tenantId)).thenReturn(Mono.just(tenant));
        when(tenantRepository.existsByName(TenantName.of("New Acme"))).thenReturn(Mono.just(false));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        UpdateAdminTenantCommand command = new UpdateAdminTenantCommand("New Acme", TenantStatus.SUSPENDED);

        StepVerifier.create(useCase.execute(command))
                .assertNext(view -> {
                    assertThat(view.name()).isEqualTo("New Acme");
                    assertThat(view.status()).isEqualTo(TenantStatus.SUSPENDED);
                })
                .verifyComplete();

        verify(tenantRepository).save(tenant);
    }

    @Test
    void shouldRejectUpdateWithoutFields() {
        Instant now = Instant.parse("2026-06-01T12:00:00Z");
        Tenant tenant = Tenant.create(tenantId, TenantName.of("Acme"), now);
        when(tenantRepository.findById(tenantId)).thenReturn(Mono.just(tenant));

        StepVerifier.create(useCase.execute(new UpdateAdminTenantCommand(null, null)))
                .expectError(InvalidDomainValueException.class)
                .verify();

        verify(tenantRepository, never()).save(any());
    }

    @Test
    void shouldRejectDuplicateTenantName() {
        Instant now = Instant.parse("2026-06-01T12:00:00Z");
        Tenant tenant = Tenant.create(tenantId, TenantName.of("Acme"), now);
        when(tenantRepository.findById(tenantId)).thenReturn(Mono.just(tenant));
        when(tenantRepository.existsByName(TenantName.of("Taken"))).thenReturn(Mono.just(true));

        StepVerifier.create(useCase.execute(new UpdateAdminTenantCommand("Taken", null)))
                .expectError(TenantAlreadyExistsException.class)
                .verify();

        verify(tenantRepository, never()).save(any());
    }
}
