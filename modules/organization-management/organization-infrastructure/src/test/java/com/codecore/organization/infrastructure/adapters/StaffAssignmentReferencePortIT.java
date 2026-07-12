package com.codecore.organization.infrastructure.adapters;

import com.codecore.organization.application.port.out.StaffAssignmentRepository;
import com.codecore.organization.contract.reference.StaffAssignmentReferencePort;
import com.codecore.organization.domain.model.staffassignment.StaffAssignment;
import com.codecore.organization.domain.valueobject.MembershipId;
import com.codecore.organization.domain.valueobject.OfficeId;
import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.organization.domain.valueobject.StaffAssignmentId;
import com.codecore.organization.domain.valueobject.TenantId;
import com.codecore.organization.infrastructure.persistence.mapper.StaffAssignmentMapper;
import com.codecore.organization.infrastructure.persistence.repository.R2dbcStaffAssignmentRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import({
        R2dbcStaffAssignmentRepository.class,
        R2dbcStaffAssignmentReferenceAdapter.class,
        StaffAssignmentReferencePortIT.TestConfig.class
})
class StaffAssignmentReferencePortIT extends AbstractPostgresIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-07-11T22:00:00Z");

    @Autowired
    private StaffAssignmentRepository staffAssignmentRepository;
    @Autowired
    private StaffAssignmentReferencePort staffAssignmentReferencePort;

    @Test
    void shouldReturnOrganizationWideScope() {
        TenantId tenantId = TenantId.generate();
        OrganizationId organizationId = OrganizationId.generate();
        StaffAssignmentId assignmentId = StaffAssignmentId.generate();

        persistAssignment(assignmentId, tenantId, organizationId, null);

        StepVerifier.create(staffAssignmentReferencePort.findScopeByIdAndTenant(assignmentId, tenantId))
                .assertNext(optional -> {
                    assertThat(optional).isPresent();
                    assertThat(optional.get().organizationId()).isEqualTo(organizationId);
                    assertThat(optional.get().officeId()).isEmpty();
                    assertThat(optional.get().isOrganizationWide()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnOfficeBoundScope() {
        TenantId tenantId = TenantId.generate();
        OrganizationId organizationId = OrganizationId.generate();
        OfficeId officeId = OfficeId.generate();
        StaffAssignmentId assignmentId = StaffAssignmentId.generate();

        persistAssignment(assignmentId, tenantId, organizationId, officeId);

        StepVerifier.create(staffAssignmentReferencePort.findScopeByIdAndTenant(assignmentId, tenantId))
                .assertNext(optional -> {
                    assertThat(optional).isPresent();
                    assertThat(optional.get().organizationId()).isEqualTo(organizationId);
                    assertThat(optional.get().officeId()).contains(officeId);
                    assertThat(optional.get().isOrganizationWide()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyForWrongTenant() {
        TenantId tenantId = TenantId.generate();
        StaffAssignmentId assignmentId = StaffAssignmentId.generate();
        persistAssignment(assignmentId, tenantId, OrganizationId.generate(), null);

        StepVerifier.create(staffAssignmentReferencePort.findScopeByIdAndTenant(
                        assignmentId,
                        TenantId.generate()
                ))
                .assertNext(optional -> assertThat(optional).isEmpty())
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyForUnknownAssignment() {
        StepVerifier.create(staffAssignmentReferencePort.findScopeByIdAndTenant(
                        StaffAssignmentId.generate(),
                        TenantId.generate()
                ))
                .assertNext(optional -> assertThat(optional).isEmpty())
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyAfterPhysicalDelete() {
        TenantId tenantId = TenantId.generate();
        StaffAssignmentId assignmentId = StaffAssignmentId.generate();
        persistAssignment(assignmentId, tenantId, OrganizationId.generate(), null);

        StepVerifier.create(staffAssignmentRepository.delete(assignmentId))
                .verifyComplete();

        StepVerifier.create(staffAssignmentReferencePort.findScopeByIdAndTenant(assignmentId, tenantId))
                .assertNext(optional -> assertThat(optional).isEmpty())
                .verifyComplete();
    }

    private void persistAssignment(
            StaffAssignmentId assignmentId,
            TenantId tenantId,
            OrganizationId organizationId,
            OfficeId officeId
    ) {
        StepVerifier.create(staffAssignmentRepository.save(StaffAssignment.create(
                        assignmentId,
                        tenantId,
                        new MembershipId(UUID.randomUUID()),
                        organizationId,
                        officeId,
                        NOW
                )))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Configuration
    @EnableR2dbcRepositories(basePackages = "com.codecore.organization.infrastructure.persistence.repository")
    static class TestConfig {

        @Bean
        StaffAssignmentMapper staffAssignmentMapper() {
            return new StaffAssignmentMapper();
        }
    }
}
