package com.codecore.iam.application.authorization;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates platform RBAC matrix (Org + Patient + Appointment + Encounter) without database.
 */
class SystemRoleTemplateTest {

    @Test
    void ownerShouldReceiveAllPlatformPermissions() {
        assertThat(SystemRoleTemplate.OWNER.permissions())
                .containsExactlyInAnyOrderElementsOf(IamPermissionCatalog.ALL);
    }

    @Test
    void adminShouldReceiveOrgPatientAppointmentEncounterAndIamAdminWithoutTenantGovernance() {
        assertThat(SystemRoleTemplate.ADMIN.permissions())
                .containsAll(IamPermissionCatalog.ADMIN_IAM)
                .containsAll(IamPermissionCatalog.ORGANIZATION_PLATFORM_ALL)
                .containsAll(IamPermissionCatalog.PATIENT_PLATFORM_ALL)
                .containsAll(IamPermissionCatalog.APPOINTMENT_PLATFORM_ALL)
                .containsAll(IamPermissionCatalog.ENCOUNTER_PLATFORM_ALL)
                .doesNotContain(
                        IamPermissionCatalog.TENANT_UPDATE,
                        IamPermissionCatalog.PERMISSION_READ
                );
    }

    @Test
    void managerShouldAdministerOfficesStaffPatientsAppointmentsAndEncountersButNotOrganizations() {
        assertThat(SystemRoleTemplate.MANAGER.permissions())
                .contains(
                        IamPermissionCatalog.ORGANIZATION_READ,
                        IamPermissionCatalog.OFFICE_CREATE,
                        IamPermissionCatalog.STAFF_ASSIGNMENT_DELETE,
                        IamPermissionCatalog.PATIENT_CREATE,
                        IamPermissionCatalog.PATIENT_UPDATE,
                        IamPermissionCatalog.PATIENT_ARCHIVE,
                        IamPermissionCatalog.APPOINTMENT_CREATE,
                        IamPermissionCatalog.APPOINTMENT_UPDATE,
                        IamPermissionCatalog.APPOINTMENT_CANCEL,
                        IamPermissionCatalog.ENCOUNTER_CREATE,
                        IamPermissionCatalog.ENCOUNTER_UPDATE,
                        IamPermissionCatalog.ENCOUNTER_CANCEL
                )
                .doesNotContain(
                        IamPermissionCatalog.ORGANIZATION_CREATE,
                        IamPermissionCatalog.ORGANIZATION_UPDATE,
                        IamPermissionCatalog.ORGANIZATION_ARCHIVE,
                        IamPermissionCatalog.TENANT_UPDATE
                );
    }

    @Test
    void userShouldReadStructurePatientsAppointmentsAndEncountersOnly() {
        assertThat(SystemRoleTemplate.USER.permissions())
                .containsExactlyInAnyOrderElementsOf(
                        IamPermissionCatalog.union(
                                Set.of(IamPermissionCatalog.USER_READ),
                                IamPermissionCatalog.STRUCTURE_READ,
                                IamPermissionCatalog.PATIENT_READ_ONLY,
                                IamPermissionCatalog.APPOINTMENT_READ_ONLY,
                                IamPermissionCatalog.ENCOUNTER_READ_ONLY
                        )
                );
    }

    @Test
    void readOnlyShouldNavigateWithoutWrites() {
        assertThat(SystemRoleTemplate.READ_ONLY.permissions())
                .containsAll(IamPermissionCatalog.STRUCTURE_READ)
                .contains(IamPermissionCatalog.PATIENT_READ)
                .contains(IamPermissionCatalog.APPOINTMENT_READ)
                .contains(IamPermissionCatalog.ENCOUNTER_READ)
                .doesNotContain(
                        IamPermissionCatalog.ORGANIZATION_CREATE,
                        IamPermissionCatalog.OFFICE_CREATE,
                        IamPermissionCatalog.STAFF_ASSIGNMENT_CREATE,
                        IamPermissionCatalog.PATIENT_CREATE,
                        IamPermissionCatalog.PATIENT_UPDATE,
                        IamPermissionCatalog.PATIENT_ARCHIVE,
                        IamPermissionCatalog.APPOINTMENT_CREATE,
                        IamPermissionCatalog.APPOINTMENT_UPDATE,
                        IamPermissionCatalog.APPOINTMENT_CANCEL,
                        IamPermissionCatalog.ENCOUNTER_CREATE,
                        IamPermissionCatalog.ENCOUNTER_UPDATE,
                        IamPermissionCatalog.ENCOUNTER_CANCEL,
                        IamPermissionCatalog.USER_UPDATE
                );
    }

    @Test
    void platformCatalogShouldIncludeOrganizationPatientAppointmentAndEncounterContracts() {
        assertThat(IamPermissionCatalog.ORGANIZATION_PLATFORM_ALL).hasSize(12);
        assertThat(IamPermissionCatalog.PATIENT_PLATFORM_ALL).hasSize(4);
        assertThat(IamPermissionCatalog.APPOINTMENT_PLATFORM_ALL).hasSize(4);
        assertThat(IamPermissionCatalog.ENCOUNTER_PLATFORM_ALL).hasSize(4);
        assertThat(IamPermissionCatalog.ALL).hasSize(40);
    }
}
