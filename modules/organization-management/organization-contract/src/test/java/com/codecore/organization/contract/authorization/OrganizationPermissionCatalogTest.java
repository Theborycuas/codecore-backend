package com.codecore.organization.contract.authorization;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrganizationPermissionCatalogTest {

    @Test
    void shouldExposeTwelveOrganizationManagementPermissions() {
        assertThat(OrganizationPermissionCatalog.ALL).hasSize(12);
        assertThat(OrganizationPermissionCatalog.ORGANIZATION_ALL).hasSize(4);
        assertThat(OrganizationPermissionCatalog.OFFICE_ALL).hasSize(4);
        assertThat(OrganizationPermissionCatalog.STAFF_ASSIGNMENT_ALL).hasSize(4);
        assertThat(OrganizationPermissionCatalog.STRUCTURE_READ).hasSize(3);
    }

    @Test
    void shouldUseArchiveNotDeleteForOrganizationsAndOffices() {
        assertThat(OrganizationPermissionCatalog.ORGANIZATION_ALL)
                .contains("organization:archive")
                .doesNotContain("organization:delete");
        assertThat(OrganizationPermissionCatalog.OFFICE_ALL)
                .contains("office:archive")
                .doesNotContain("office:delete");
    }
}
