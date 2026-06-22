package com.codecore.iam.application.authorization;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates Organization Management RBAC matrix (FASE 16.3) without database.
 */
class SystemRoleTemplateTest {

    @Test
    void ownerShouldReceiveAllPlatformPermissions() {
        assertThat(SystemRoleTemplate.OWNER.permissions())
                .containsExactlyInAnyOrderElementsOf(IamPermissionCatalog.ALL);
    }

    @Test
    void adminShouldReceiveOrganizationPlatformAndIamAdminWithoutTenantGovernance() {
        assertThat(SystemRoleTemplate.ADMIN.permissions())
                .containsAll(IamPermissionCatalog.ADMIN_IAM)
                .containsAll(IamPermissionCatalog.ORGANIZATION_PLATFORM_ALL)
                .doesNotContain(
                        IamPermissionCatalog.TENANT_UPDATE,
                        IamPermissionCatalog.PERMISSION_READ
                );
    }

    @Test
    void managerShouldAdministerOfficesAndStaffAssignmentsButNotOrganizations() {
        assertThat(SystemRoleTemplate.MANAGER.permissions())
                .contains(
                        IamPermissionCatalog.ORGANIZATION_READ,
                        IamPermissionCatalog.OFFICE_CREATE,
                        IamPermissionCatalog.STAFF_ASSIGNMENT_DELETE
                )
                .doesNotContain(
                        IamPermissionCatalog.ORGANIZATION_CREATE,
                        IamPermissionCatalog.ORGANIZATION_UPDATE,
                        IamPermissionCatalog.ORGANIZATION_ARCHIVE,
                        IamPermissionCatalog.TENANT_UPDATE
                );
    }

    @Test
    void userShouldReadStructureOnly() {
        assertThat(SystemRoleTemplate.USER.permissions())
                .containsExactlyInAnyOrderElementsOf(
                        IamPermissionCatalog.union(
                                Set.of(IamPermissionCatalog.USER_READ),
                                IamPermissionCatalog.STRUCTURE_READ
                        )
                );
    }

    @Test
    void readOnlyShouldNavigateStructureWithoutWrites() {
        assertThat(SystemRoleTemplate.READ_ONLY.permissions())
                .containsAll(IamPermissionCatalog.STRUCTURE_READ)
                .doesNotContain(
                        IamPermissionCatalog.ORGANIZATION_CREATE,
                        IamPermissionCatalog.OFFICE_CREATE,
                        IamPermissionCatalog.STAFF_ASSIGNMENT_CREATE,
                        IamPermissionCatalog.USER_UPDATE
                );
    }

    @Test
    void organizationPlatformCatalogShouldHaveTwelvePermissions() {
        assertThat(IamPermissionCatalog.ORGANIZATION_PLATFORM_ALL).hasSize(12);
        assertThat(IamPermissionCatalog.ALL).hasSize(28);
    }
}
