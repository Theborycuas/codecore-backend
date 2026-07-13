package com.codecore.billing.contract.reference;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceReferencePortContractTest {

    @Test
    void shouldExposeOnlyExistsIssuedByIdAndTenant() {
        Method[] methods = InvoiceReferencePort.class.getDeclaredMethods();
        assertThat(methods).hasSize(1);
        assertThat(methods[0].getName()).isEqualTo("existsIssuedByIdAndTenant");
        assertThat(methods[0].getParameterCount()).isEqualTo(2);
    }
}
