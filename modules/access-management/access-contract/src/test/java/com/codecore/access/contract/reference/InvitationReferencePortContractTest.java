package com.codecore.access.contract.reference;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class InvitationReferencePortContractTest {

    @Test
    void shouldExposeOnlyExistsPendingByIdAndTenant() {
        Method[] methods = InvitationReferencePort.class.getDeclaredMethods();
        assertThat(methods).hasSize(1);
        assertThat(methods[0].getName()).isEqualTo("existsPendingByIdAndTenant");
        assertThat(methods[0].getParameterCount()).isEqualTo(2);
    }
}
