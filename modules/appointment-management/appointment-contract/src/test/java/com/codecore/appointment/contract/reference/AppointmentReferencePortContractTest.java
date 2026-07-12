package com.codecore.appointment.contract.reference;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentReferencePortContractTest {

    @Test
    void shouldExposeOnlyExistsScheduledByIdAndTenant() {
        Method[] methods = AppointmentReferencePort.class.getDeclaredMethods();
        assertThat(methods).hasSize(1);
        assertThat(methods[0].getName()).isEqualTo("existsScheduledByIdAndTenant");
        assertThat(methods[0].getParameterCount()).isEqualTo(2);
    }
}
