package com.codecore.organization.contract.reference;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

class StaffAssignmentReferencePortContractTest {

    @Test
    void shouldExposeOnlyFindScopeByIdAndTenant() {
        Method[] methods = StaffAssignmentReferencePort.class.getDeclaredMethods();
        assertThat(methods).hasSize(1);
        assertThat(methods[0].getName()).isEqualTo("findScopeByIdAndTenant");
        assertThat(methods[0].getParameterCount()).isEqualTo(2);
        assertThat(Modifier.isPublic(methods[0].getModifiers())).isTrue();
    }

    @Test
    void referenceViewShouldExposeOnlyScopeIds() {
        Method[] methods = StaffAssignmentReferenceView.class.getDeclaredMethods();
        assertThat(methods)
                .extracting(Method::getName)
                .contains("staffAssignmentId", "organizationId", "officeId", "isOrganizationWide");
        assertThat(methods)
                .extracting(Method::getName)
                .doesNotContain("membershipId", "save", "delete", "changeScope");
    }
}
