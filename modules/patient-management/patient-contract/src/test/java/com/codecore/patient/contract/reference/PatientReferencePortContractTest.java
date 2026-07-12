package com.codecore.patient.contract.reference;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class PatientReferencePortContractTest {

    @Test
    void shouldExposeOnlyExistsActiveByIdAndTenant() {
        Method[] methods = PatientReferencePort.class.getDeclaredMethods();
        assertThat(methods).hasSize(1);
        assertThat(methods[0].getName()).isEqualTo("existsActiveByIdAndTenant");
        assertThat(methods[0].getParameterCount()).isEqualTo(2);
    }
}
