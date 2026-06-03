package com.codecore.iam.infrastructure.persistence.repository;

import com.codecore.iam.application.port.out.TenantRepository;
import com.codecore.iam.domain.model.tenant.Tenant;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.domain.valueobject.TenantName;
import com.codecore.iam.domain.valueobject.TenantStatus;
import com.codecore.iam.testsupport.AbstractPostgresIntegrationTest;
import com.codecore.iam.testsupport.IamTenantPersistenceTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import(IamTenantPersistenceTestConfiguration.class)
class R2dbcTenantRepositoryIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void shouldPersistFindAndCheckExistsById() {
        TenantId tenantId = TenantId.generate();
        Instant now = Instant.now();
        Tenant tenant = Tenant.create(tenantId, TenantName.of("Persisted Tenant %s".formatted(tenantId.value())), now);

        StepVerifier.create(tenantRepository.save(tenant))
                .assertNext(saved -> {
                    assertThat(saved.id()).isEqualTo(tenantId);
                    assertThat(saved.name().value()).isEqualTo(tenant.name().value());
                    assertThat(saved.status()).isEqualTo(TenantStatus.ACTIVE);
                    assertThat(saved.createdAt()).isEqualTo(now);
                })
                .verifyComplete();

        StepVerifier.create(tenantRepository.existsById(tenantId))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(tenantRepository.findById(tenantId))
                .assertNext(found -> {
                    assertThat(found.id()).isEqualTo(tenantId);
                    assertThat(found.status()).isEqualTo(TenantStatus.ACTIVE);
                    assertThat(found.name().value()).isEqualTo(tenant.name().value());
                })
                .verifyComplete();

        tenant.suspend();
        StepVerifier.create(tenantRepository.save(tenant))
                .assertNext(updated -> assertThat(updated.status()).isEqualTo(TenantStatus.SUSPENDED))
                .verifyComplete();

        StepVerifier.create(tenantRepository.findById(tenantId))
                .assertNext(reloaded -> assertThat(reloaded.status()).isEqualTo(TenantStatus.SUSPENDED))
                .verifyComplete();
    }

    @Test
    void shouldReportExistsByName() {
        TenantId tenantId = TenantId.generate();
        TenantName name = TenantName.of("Exists By Name %s".formatted(tenantId.value()));
        Tenant tenant = Tenant.create(tenantId, name, Instant.now());

        StepVerifier.create(tenantRepository.save(tenant))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(tenantRepository.existsByName(name))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(tenantRepository.existsByName(TenantName.of("Other Name")))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenTenantDoesNotExist() {
        TenantId unknown = TenantId.generate();

        StepVerifier.create(tenantRepository.findById(unknown))
                .verifyComplete();

        StepVerifier.create(tenantRepository.existsById(unknown))
                .expectNext(false)
                .verifyComplete();
    }
}
