package com.codecore.organization.infrastructure.persistence.repository;

import com.codecore.organization.application.port.out.OrganizationQueryPort;
import com.codecore.organization.application.port.out.OrganizationRepository;
import com.codecore.organization.domain.model.organization.Organization;
import com.codecore.organization.domain.valueobject.OrganizationCode;
import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.organization.domain.valueobject.OrganizationName;
import com.codecore.organization.domain.valueobject.OrganizationStatus;
import com.codecore.organization.domain.valueobject.TenantId;
import com.codecore.organization.testsupport.AbstractPostgresIntegrationTest;
import com.codecore.organization.testsupport.OrganizationPersistenceTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import(OrganizationPersistenceTestConfiguration.class)
class R2dbcOrganizationRepositoryIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationQueryPort organizationQueryPort;

    @Test
    void shouldPersistAndFindById() {
        OrganizationId organizationId = OrganizationId.generate();
        TenantId tenantId = TenantId.generate();
        Instant now = Instant.parse("2026-06-22T10:00:00Z");
        Organization organization = Organization.create(
                organizationId,
                tenantId,
                OrganizationCode.of("DENTAL_NORTE"),
                OrganizationName.of("Dental Norte"),
                now
        );

        StepVerifier.create(organizationRepository.save(organization))
                .assertNext(saved -> {
                    assertThat(saved.id()).isEqualTo(organizationId);
                    assertThat(saved.tenantId()).isEqualTo(tenantId);
                    assertThat(saved.code().value()).isEqualTo("DENTAL_NORTE");
                    assertThat(saved.name().value()).isEqualTo("Dental Norte");
                    assertThat(saved.status()).isEqualTo(OrganizationStatus.ACTIVE);
                    assertThat(saved.createdAt()).isEqualTo(now);
                })
                .verifyComplete();

        StepVerifier.create(organizationRepository.findById(organizationId))
                .assertNext(found -> {
                    assertThat(found.id()).isEqualTo(organizationId);
                    assertThat(found.tenantId()).isEqualTo(tenantId);
                    assertThat(found.code().value()).isEqualTo("DENTAL_NORTE");
                })
                .verifyComplete();
    }

    @Test
    void shouldReportExistsByTenantIdAndCode() {
        TenantId tenantId = TenantId.generate();
        OrganizationCode code = OrganizationCode.of("CARDIOLOGIA");
        Organization organization = Organization.create(
                OrganizationId.generate(),
                tenantId,
                code,
                OrganizationName.of("Cardiología"),
                Instant.now()
        );

        StepVerifier.create(organizationRepository.save(organization))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(organizationRepository.existsByTenantIdAndCode(tenantId, code))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(organizationRepository.existsByTenantIdAndCode(
                tenantId,
                OrganizationCode.of("EMERGENCIAS")))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldAllowSameCodeInDifferentTenants() {
        OrganizationCode code = OrganizationCode.of("DENTAL_SUR");
        Instant now = Instant.now();

        Organization tenantA = Organization.create(
                OrganizationId.generate(),
                TenantId.generate(),
                code,
                OrganizationName.of("Dental Sur A"),
                now
        );
        Organization tenantB = Organization.create(
                OrganizationId.generate(),
                TenantId.generate(),
                code,
                OrganizationName.of("Dental Sur B"),
                now
        );

        StepVerifier.create(organizationRepository.save(tenantA))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(organizationRepository.save(tenantB))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldRejectDuplicateCodeWithinSameTenant() {
        TenantId tenantId = TenantId.generate();
        OrganizationCode code = OrganizationCode.of("EMERGENCIAS");
        Instant now = Instant.now();

        StepVerifier.create(organizationRepository.save(
                Organization.create(
                        OrganizationId.generate(),
                        tenantId,
                        code,
                        OrganizationName.of("Emergencias Principal"),
                        now)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(organizationRepository.save(
                Organization.create(
                        OrganizationId.generate(),
                        tenantId,
                        code,
                        OrganizationName.of("Emergencias Duplicada"),
                        now)))
                .expectError(DuplicateKeyException.class)
                .verify();
    }

    @Test
    void shouldFindByIdAndTenantId() {
        OrganizationId organizationId = OrganizationId.generate();
        TenantId tenantId = TenantId.generate();
        TenantId otherTenantId = TenantId.generate();
        Organization organization = Organization.create(
                organizationId,
                tenantId,
                OrganizationCode.of("PEDIATRIA"),
                OrganizationName.of("Pediatría"),
                Instant.now()
        );

        StepVerifier.create(organizationRepository.save(organization))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(organizationQueryPort.findByIdAndTenantId(organizationId, tenantId))
                .assertNext(found -> assertThat(found.id()).isEqualTo(organizationId))
                .verifyComplete();

        StepVerifier.create(organizationQueryPort.findByIdAndTenantId(organizationId, otherTenantId))
                .verifyComplete();
    }

    @Test
    void shouldCountByTenantId() {
        TenantId tenantId = TenantId.generate();
        TenantId otherTenantId = TenantId.generate();
        Instant now = Instant.now();

        StepVerifier.create(organizationRepository.save(
                Organization.create(
                        OrganizationId.generate(),
                        tenantId,
                        OrganizationCode.of("ORG_ONE"),
                        OrganizationName.of("Org One"),
                        now)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(organizationRepository.save(
                Organization.create(
                        OrganizationId.generate(),
                        tenantId,
                        OrganizationCode.of("ORG_TWO"),
                        OrganizationName.of("Org Two"),
                        now)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(organizationRepository.save(
                Organization.create(
                        OrganizationId.generate(),
                        otherTenantId,
                        OrganizationCode.of("ORG_ONE"),
                        OrganizationName.of("Other Tenant Org"),
                        now)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(organizationQueryPort.countByTenantId(tenantId))
                .expectNext(2L)
                .verifyComplete();

        StepVerifier.create(organizationQueryPort.findAllByTenantId(tenantId))
                .expectNextCount(2)
                .verifyComplete();
    }
}
