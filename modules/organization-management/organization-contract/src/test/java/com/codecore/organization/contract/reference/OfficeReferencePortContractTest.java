package com.codecore.organization.contract.reference;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

class OfficeReferencePortContractTest {

    @Test
    void shouldExposeOnlyExistsActiveInOrganization() {
        Method[] methods = OfficeReferencePort.class.getDeclaredMethods();
        assertThat(methods).hasSize(1);
        assertThat(methods[0].getName()).isEqualTo("existsActiveInOrganization");
        assertThat(methods[0].getParameterCount()).isEqualTo(3);
        assertThat(Modifier.isPublic(methods[0].getModifiers())).isTrue();
    }
}
