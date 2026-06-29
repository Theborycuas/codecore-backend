package com.codecore.organization.infrastructure.persistence.repository;

import com.codecore.organization.application.port.out.StaffAssignmentQueryPort;
import com.codecore.organization.application.port.out.StaffAssignmentRepository;
import com.codecore.organization.domain.model.staffassignment.StaffAssignment;
import com.codecore.organization.domain.valueobject.MembershipId;
import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.organization.domain.valueobject.StaffAssignmentId;
import com.codecore.organization.domain.valueobject.TenantId;
import com.codecore.organization.infrastructure.persistence.mapper.StaffAssignmentMapper;
import com.codecore.organization.testsupport.AbstractPostgresIntegrationTest;
import com.codecore.organization.testsupport.StaffAssignmentPersistenceTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import(StaffAssignmentPersistenceTestConfiguration.class)
class R2dbcStaffAssignmentRepositoryIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private StaffAssignmentRepository staffAssignmentRepository;

    @Autowired
    private StaffAssignmentQueryPort staffAssignmentQueryPort;

    @Test
    void shouldPersistAndFindByIdAndTenant() {
        StaffAssignmentId assignmentId = StaffAssignmentId.generate();
        TenantId tenantId = TenantId.generate();
        MembershipId membershipId = new MembershipId(java.util.UUID.randomUUID());
        OrganizationId organizationId = OrganizationId.generate();
        Instant now = Instant.parse("2026-06-22T10:00:00Z");

        StaffAssignment assignment = StaffAssignment.create(
                assignmentId,
                tenantId,
                membershipId,
                organizationId,
                null,
                now
        );

        StepVerifier.create(staffAssignmentRepository.save(assignment))
                .assertNext(saved -> {
                    assertThat(saved.id()).isEqualTo(assignmentId);
                    assertThat(saved.tenantId()).isEqualTo(tenantId);
                    assertThat(saved.membershipId()).isEqualTo(membershipId);
                    assertThat(saved.organizationId()).isEqualTo(organizationId);
                    assertThat(saved.officeId()).isNull();
                    assertThat(saved.isOrganizationLevel()).isTrue();
                })
                .verifyComplete();

        StepVerifier.create(staffAssignmentQueryPort.findByIdAndTenantId(assignmentId, tenantId))
                .assertNext(found -> assertThat(found.id()).isEqualTo(assignmentId))
                .verifyComplete();
    }

    @Test
    void shouldReportExistsByOrgScope() {
        TenantId tenantId = TenantId.generate();
        MembershipId membershipId = new MembershipId(java.util.UUID.randomUUID());
        OrganizationId organizationId = OrganizationId.generate();
        StaffAssignment assignment = StaffAssignment.create(
                StaffAssignmentId.generate(),
                tenantId,
                membershipId,
                organizationId,
                null,
                Instant.now()
        );

        StepVerifier.create(staffAssignmentRepository.save(assignment))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(staffAssignmentRepository.existsByScope(
                tenantId,
                membershipId,
                organizationId,
                null
        ))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldRejectDuplicateOrgScope() {
        TenantId tenantId = TenantId.generate();
        MembershipId membershipId = new MembershipId(java.util.UUID.randomUUID());
        OrganizationId organizationId = OrganizationId.generate();
        Instant now = Instant.now();

        StaffAssignment first = StaffAssignment.create(
                StaffAssignmentId.generate(),
                tenantId,
                membershipId,
                organizationId,
                null,
                now
        );
        StaffAssignment duplicate = StaffAssignment.create(
                StaffAssignmentId.generate(),
                tenantId,
                membershipId,
                organizationId,
                null,
                now
        );

        StepVerifier.create(staffAssignmentRepository.save(first))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(staffAssignmentRepository.save(duplicate))
                .expectError(DuplicateKeyException.class)
                .verify();
    }

    @Test
    void shouldDeleteAssignment() {
        StaffAssignmentId assignmentId = StaffAssignmentId.generate();
        TenantId tenantId = TenantId.generate();
        StaffAssignment assignment = StaffAssignment.create(
                assignmentId,
                tenantId,
                new MembershipId(java.util.UUID.randomUUID()),
                OrganizationId.generate(),
                null,
                Instant.now()
        );

        StepVerifier.create(staffAssignmentRepository.save(assignment))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(staffAssignmentRepository.delete(assignmentId))
                .verifyComplete();

        StepVerifier.create(staffAssignmentQueryPort.findByIdAndTenantId(assignmentId, tenantId))
                .verifyComplete();
    }
}
