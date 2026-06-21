package com.codecore.iam.application;

import com.codecore.iam.application.port.out.TenantRepository;
import com.codecore.iam.domain.exception.TenantNotFoundException;
import com.codecore.iam.domain.exception.TenantNotOperationalException;
import com.codecore.iam.domain.model.tenant.Tenant;
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

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantOperationalGuardTest {

    @Mock
    private TenantRepository tenantRepository;

    private TenantOperationalGuard guard;

    @BeforeEach
    void setUp() {
        guard = new TenantOperationalGuard(tenantRepository);
    }

    @Test
    void shouldAllowActiveTenant() {
        TenantId tenantId = TenantId.generate();
        Tenant tenant = new Tenant(
                tenantId,
                TenantName.of("Acme"),
                TenantStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );
        when(tenantRepository.findById(tenantId)).thenReturn(Mono.just(tenant));

        StepVerifier.create(guard.assertOperational(tenantId))
                .verifyComplete();
    }

    @Test
    void shouldRejectSuspendedTenant() {
        TenantId tenantId = TenantId.generate();
        Tenant tenant = Tenant.create(tenantId, TenantName.of("Acme"), Instant.now());
        tenant.suspend();
        when(tenantRepository.findById(tenantId)).thenReturn(Mono.just(tenant));

        StepVerifier.create(guard.assertOperational(tenantId))
                .expectError(TenantNotOperationalException.class)
                .verify();
    }

    @Test
    void shouldRejectWhenTenantMissing() {
        TenantId tenantId = TenantId.generate();
        when(tenantRepository.findById(tenantId)).thenReturn(Mono.empty());

        StepVerifier.create(guard.assertOperational(tenantId))
                .expectError(TenantNotFoundException.class)
                .verify();
    }
}
