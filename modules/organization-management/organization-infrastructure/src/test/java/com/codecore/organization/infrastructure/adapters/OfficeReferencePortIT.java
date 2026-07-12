package com.codecore.organization.infrastructure.adapters;

import com.codecore.organization.application.port.out.OfficeRepository;
import com.codecore.organization.application.port.out.OrganizationRepository;
import com.codecore.organization.contract.reference.OfficeReferencePort;
import com.codecore.organization.domain.model.office.Office;
import com.codecore.organization.domain.model.organization.Organization;
import com.codecore.organization.domain.valueobject.OfficeCode;
import com.codecore.organization.domain.valueobject.OfficeId;
import com.codecore.organization.domain.valueobject.OfficeName;
import com.codecore.organization.domain.valueobject.OrganizationCode;
import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.organization.domain.valueobject.OrganizationName;
import com.codecore.organization.domain.valueobject.TenantId;
import com.codecore.organization.infrastructure.persistence.mapper.OfficeMapper;
import com.codecore.organization.infrastructure.persistence.mapper.OrganizationMapper;
import com.codecore.organization.infrastructure.persistence.repository.R2dbcOfficeRepository;
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
        R2dbcOfficeRepository.class,
        R2dbcOfficeReferenceAdapter.class,
        OfficeReferencePortIT.TestConfig.class
})
class OfficeReferencePortIT extends AbstractPostgresIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-07-11T22:00:00Z");

    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private OfficeRepository officeRepository;
    @Autowired
    private OfficeReferencePort officeReferencePort;

    @Test
    void shouldReturnTrueForActiveOfficeInOrganizationAndTenant() {
        TenantId tenantId = TenantId.generate();
        OrganizationId organizationId = OrganizationId.generate();
        OfficeId officeId = OfficeId.generate();

        persistOrganization(organizationId, tenantId, "OFF_REF_ORG", "Office Ref Org");
        persistOffice(officeId, tenantId, organizationId, "ROOM_1", "Room 1");

        StepVerifier.create(officeReferencePort.existsActiveInOrganization(officeId, organizationId, tenantId))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldReturnFalseForWrongTenant() {
        TenantId tenantId = TenantId.generate();
        OrganizationId organizationId = OrganizationId.generate();
        OfficeId officeId = OfficeId.generate();

        persistOrganization(organizationId, tenantId, "OFF_TENANT", "Tenant Org");
        persistOffice(officeId, tenantId, organizationId, "ROOM_T", "Room T");

        StepVerifier.create(officeReferencePort.existsActiveInOrganization(
                        officeId,
                        organizationId,
                        TenantId.generate()
                ))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldReturnFalseForOrganizationMismatch() {
        TenantId tenantId = TenantId.generate();
        OrganizationId organizationId = OrganizationId.generate();
        OrganizationId otherOrganizationId = OrganizationId.generate();
        OfficeId officeId = OfficeId.generate();

        persistOrganization(organizationId, tenantId, "OFF_ORG_A", "Org A");
        persistOrganization(otherOrganizationId, tenantId, "OFF_ORG_B", "Org B");
        persistOffice(officeId, tenantId, organizationId, "ROOM_A", "Room A");

        StepVerifier.create(officeReferencePort.existsActiveInOrganization(
                        officeId,
                        otherOrganizationId,
                        tenantId
                ))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldReturnFalseWhenOfficeArchived() {
        TenantId tenantId = TenantId.generate();
        OrganizationId organizationId = OrganizationId.generate();
        OfficeId officeId = OfficeId.generate();

        persistOrganization(organizationId, tenantId, "OFF_ARCH", "Arch Org");
        StepVerifier.create(officeRepository.save(Office.create(
                        officeId,
                        tenantId,
                        organizationId,
                        OfficeCode.of("ARCH_ROOM"),
                        OfficeName.of("Archived Room"),
                        NOW
                )).flatMap(saved -> {
                    saved.archive();
                    return officeRepository.save(saved);
                }))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(officeReferencePort.existsActiveInOrganization(officeId, organizationId, tenantId))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldReturnFalseForUnknownOffice() {
        TenantId tenantId = TenantId.generate();
        OrganizationId organizationId = OrganizationId.generate();
        persistOrganization(organizationId, tenantId, "OFF_UNK", "Unknown Org");

        StepVerifier.create(officeReferencePort.existsActiveInOrganization(
                        OfficeId.generate(),
                        organizationId,
                        tenantId
                ))
                .expectNext(false)
                .verifyComplete();
    }

    private void persistOrganization(
            OrganizationId organizationId,
            TenantId tenantId,
            String code,
            String name
    ) {
        StepVerifier.create(organizationRepository.save(Organization.create(
                        organizationId,
                        tenantId,
                        OrganizationCode.of(code),
                        OrganizationName.of(name),
                        NOW
                )))
                .expectNextCount(1)
                .verifyComplete();
    }

    private void persistOffice(
            OfficeId officeId,
            TenantId tenantId,
            OrganizationId organizationId,
            String code,
            String name
    ) {
        StepVerifier.create(officeRepository.save(Office.create(
                        officeId,
                        tenantId,
                        organizationId,
                        OfficeCode.of(code),
                        OfficeName.of(name),
                        NOW
                )))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Configuration
    @EnableR2dbcRepositories(basePackages = "com.codecore.organization.infrastructure.persistence.repository")
    static class TestConfig {

        @Bean
        OrganizationMapper organizationMapper() {
            return new OrganizationMapper();
        }

        @Bean
        OfficeMapper officeMapper() {
            return new OfficeMapper();
        }
    }
}
