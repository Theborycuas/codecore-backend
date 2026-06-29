package com.codecore.organization.domain.model.staffassignment;

import com.codecore.organization.domain.exception.InvalidStaffAssignmentScopeException;
import com.codecore.organization.domain.valueobject.MembershipId;
import com.codecore.organization.domain.valueobject.OfficeId;
import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.organization.domain.valueobject.StaffAssignmentId;
import com.codecore.organization.domain.valueobject.TenantId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StaffAssignmentTest {

    private static final Instant NOW = Instant.parse("2026-06-28T12:00:00Z");

    @Test
    void shouldCreateOrganizationLevelAssignment() {
        StaffAssignment assignment = StaffAssignment.create(
                StaffAssignmentId.generate(),
                TenantId.generate(),
                new MembershipId(UUID.randomUUID()),
                OrganizationId.generate(),
                null,
                NOW
        );

        assertThat(assignment.isOrganizationLevel()).isTrue();
        assertThat(assignment.officeId()).isNull();
    }

    @Test
    void shouldCreateOfficeLevelAssignment() {
        OrganizationId orgId = OrganizationId.generate();
        StaffAssignment assignment = StaffAssignment.create(
                StaffAssignmentId.generate(),
                TenantId.generate(),
                new MembershipId(UUID.randomUUID()),
                orgId,
                OfficeId.generate(),
                NOW
        );

        assertThat(assignment.isOrganizationLevel()).isFalse();
        assertThat(assignment.organizationId()).isEqualTo(orgId);
    }

    @Test
    void shouldChangeScope() {
        StaffAssignment assignment = organizationLevelAssignment();
        OrganizationId newOrg = OrganizationId.generate();
        OfficeId newOffice = OfficeId.generate();

        assignment.changeScope(newOrg, newOffice);

        assertThat(assignment.organizationId()).isEqualTo(newOrg);
        assertThat(assignment.officeId()).isEqualTo(newOffice);
        assertThat(assignment.updatedAt()).isAfter(NOW);
    }

    @Test
    void shouldRejectOfficeNotInOrganization() {
        assertThatThrownBy(() -> StaffAssignment.assertOfficeBelongsToOrganization(
                OrganizationId.generate(),
                OfficeId.generate(),
                OrganizationId.generate()
        )).isInstanceOf(InvalidStaffAssignmentScopeException.class);
    }

    private static StaffAssignment organizationLevelAssignment() {
        return StaffAssignment.create(
                StaffAssignmentId.generate(),
                TenantId.generate(),
                new MembershipId(UUID.randomUUID()),
                OrganizationId.generate(),
                null,
                NOW
        );
    }
}
