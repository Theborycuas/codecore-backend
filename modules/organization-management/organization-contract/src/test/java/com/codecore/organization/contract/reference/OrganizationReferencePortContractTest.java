package com.codecore.organization.contract.reference;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class OrganizationReferencePortContractTest {

    @Test
    void shouldExposeOnlyExistsActiveByIdAndTenant() {
        Method[] methods = OrganizationReferencePort.class.getDeclaredMethods();
        assertThat(methods).hasSize(1);
        assertThat(methods[0].getName()).isEqualTo("existsActiveByIdAndTenant");
        assertThat(methods[0].getParameterCount()).isEqualTo(2);
    }

    @Test
    void officeReferencePortShouldRemainReadOnlyExistenceCheck() {
        Method[] methods = OfficeReferencePort.class.getDeclaredMethods();
        assertThat(methods).hasSize(1);
        assertThat(methods[0].getName()).isEqualTo("existsActiveInOrganization");
    }
}
