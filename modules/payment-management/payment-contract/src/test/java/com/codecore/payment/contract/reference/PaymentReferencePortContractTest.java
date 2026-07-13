package com.codecore.payment.contract.reference;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentReferencePortContractTest {

    @Test
    void shouldExposeOnlyExistsRecordedByIdAndTenant() {
        Method[] methods = PaymentReferencePort.class.getDeclaredMethods();
        assertThat(methods).hasSize(1);
        assertThat(methods[0].getName()).isEqualTo("existsRecordedByIdAndTenant");
        assertThat(methods[0].getParameterCount()).isEqualTo(2);
    }
}
