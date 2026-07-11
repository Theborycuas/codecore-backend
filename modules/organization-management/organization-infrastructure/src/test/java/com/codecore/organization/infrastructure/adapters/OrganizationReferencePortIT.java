package com.codecore.organization.infrastructure.adapters;

import com.codecore.organization.application.port.out.OrganizationRepository;
import com.codecore.organization.contract.reference.OrganizationReferencePort;
import com.codecore.organization.domain.model.organization.Organization;
import com.codecore.organization.domain.valueobject.OrganizationCode;
import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.organization.domain.valueobject.OrganizationName;
import com.codecore.organization.domain.valueobject.TenantId;
import com.codecore.organization.infrastructure.persistence.mapper.OrganizationMapper;
import com.codecore.organization.infrastructure.persistence.repository.R2dbcOrganizationRepository;
import com.codecore.organization.testsupport.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import reactor.test.StepVerifier;

import java.time.Instant;

@DataR2dbcTest
@Import({
        R2dbcOrganizationRepository.class,
        R2dbcOrganizationReferenceAdapter.class,
        OrganizationReferencePortIT.TestConfig.class
})
class OrganizationReferencePortIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationReferencePort organizationReferencePort;

    @Test
    void shouldReturnTrueForActiveOrganizationInTenant() {
        OrganizationId organizationId = OrganizationId.generate();
        TenantId tenantId = TenantId.generate();
        Instant now = Instant.parse("2026-07-11T12:00:00Z");

        StepVerifier.create(organizationRepository.save(Organization.create(
                        organizationId,
                        tenantId,
                        OrganizationCode.of("REF_ORG"),
                        OrganizationName.of("Reference Org"),
                        now
                )))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(organizationReferencePort.existsActiveByIdAndTenant(organizationId, tenantId))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldReturnFalseForWrongTenantOrUnknownId() {
        OrganizationId organizationId = OrganizationId.generate();
        TenantId tenantId = TenantId.generate();
        Instant now = Instant.parse("2026-07-11T12:00:00Z");

        StepVerifier.create(organizationRepository.save(Organization.create(
                        organizationId,
                        tenantId,
                        OrganizationCode.of("REF_ORG_2"),
                        OrganizationName.of("Reference Org 2"),
                        now
                )))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(organizationReferencePort.existsActiveByIdAndTenant(
                        organizationId,
                        TenantId.generate()
                ))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(organizationReferencePort.existsActiveByIdAndTenant(
                        OrganizationId.generate(),
                        tenantId
                ))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldReturnFalseWhenOrganizationArchived() {
        OrganizationId organizationId = OrganizationId.generate();
        TenantId tenantId = TenantId.generate();
        Instant now = Instant.parse("2026-07-11T12:00:00Z");

        StepVerifier.create(organizationRepository.save(Organization.create(
                        organizationId,
                        tenantId,
                        OrganizationCode.of("REF_ARCH"),
                        OrganizationName.of("Archived Org"),
                        now
                )).flatMap(saved -> {
                    saved.archive();
                    return organizationRepository.save(saved);
                }))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(organizationReferencePort.existsActiveByIdAndTenant(organizationId, tenantId))
                .expectNext(false)
                .verifyComplete();
    }

    @Configuration
    @EnableR2dbcRepositories(basePackages = "com.codecore.organization.infrastructure.persistence.repository")
    static class TestConfig {

        @Bean
        OrganizationMapper organizationMapper() {
            return new OrganizationMapper();
        }
    }
}
