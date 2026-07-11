package com.codecore.iam.application.authorization;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates platform RBAC matrix (FASE 16.3 Organization + FASE 17.5 Patient) without database.
 */
class SystemRoleTemplateTest {

    @Test
    void ownerShouldReceiveAllPlatformPermissions() {
        assertThat(SystemRoleTemplate.OWNER.permissions())
                .containsExactlyInAnyOrderElementsOf(IamPermissionCatalog.ALL);
    }

    @Test
    void adminShouldReceiveOrgPatientAndIamAdminWithoutTenantGovernance() {
        assertThat(SystemRoleTemplate.ADMIN.permissions())
                .containsAll(IamPermissionCatalog.ADMIN_IAM)
                .containsAll(IamPermissionCatalog.ORGANIZATION_PLATFORM_ALL)
                .containsAll(IamPermissionCatalog.PATIENT_PLATFORM_ALL)
                .doesNotContain(
                        IamPermissionCatalog.TENANT_UPDATE,
                        IamPermissionCatalog.PERMISSION_READ
                );
    }

    @Test
    void managerShouldAdministerOfficesStaffAndPatientsButNotOrganizations() {
        assertThat(SystemRoleTemplate.MANAGER.permissions())
                .contains(
                        IamPermissionCatalog.ORGANIZATION_READ,
                        IamPermissionCatalog.OFFICE_CREATE,
                        IamPermissionCatalog.STAFF_ASSIGNMENT_DELETE,
                        IamPermissionCatalog.PATIENT_CREATE,
                        IamPermissionCatalog.PATIENT_UPDATE,
                        IamPermissionCatalog.PATIENT_ARCHIVE
                )
                .doesNotContain(
                        IamPermissionCatalog.ORGANIZATION_CREATE,
                        IamPermissionCatalog.ORGANIZATION_UPDATE,
                        IamPermissionCatalog.ORGANIZATION_ARCHIVE,
                        IamPermissionCatalog.TENANT_UPDATE
                );
    }

    @Test
    void userShouldReadStructureAndPatientsOnly() {
        assertThat(SystemRoleTemplate.USER.permissions())
                .containsExactlyInAnyOrderElementsOf(
                        IamPermissionCatalog.union(
                                Set.of(IamPermissionCatalog.USER_READ),
                                IamPermissionCatalog.STRUCTURE_READ,
                                IamPermissionCatalog.PATIENT_READ_ONLY
                        )
                );
    }

    @Test
    void readOnlyShouldNavigateWithoutWrites() {
        assertThat(SystemRoleTemplate.READ_ONLY.permissions())
                .containsAll(IamPermissionCatalog.STRUCTURE_READ)
                .contains(IamPermissionCatalog.PATIENT_READ)
                .doesNotContain(
                        IamPermissionCatalog.ORGANIZATION_CREATE,
                        IamPermissionCatalog.OFFICE_CREATE,
                        IamPermissionCatalog.STAFF_ASSIGNMENT_CREATE,
                        IamPermissionCatalog.PATIENT_CREATE,
                        IamPermissionCatalog.PATIENT_UPDATE,
                        IamPermissionCatalog.PATIENT_ARCHIVE,
                        IamPermissionCatalog.USER_UPDATE
                );
    }

    @Test
    void platformCatalogShouldIncludeOrganizationAndPatientContracts() {
        assertThat(IamPermissionCatalog.ORGANIZATION_PLATFORM_ALL).hasSize(12);
        assertThat(IamPermissionCatalog.PATIENT_PLATFORM_ALL).hasSize(4);
        assertThat(IamPermissionCatalog.ALL).hasSize(32);
    }
}
