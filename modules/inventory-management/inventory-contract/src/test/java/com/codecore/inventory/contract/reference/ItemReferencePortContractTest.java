package com.codecore.inventory.contract.reference;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class ItemReferencePortContractTest {

    @Test
    void shouldExposeOnlyExistsActiveByIdAndTenant() {
        Method[] methods = ItemReferencePort.class.getDeclaredMethods();
        assertThat(methods).hasSize(1);
        assertThat(methods[0].getName()).isEqualTo("existsActiveByIdAndTenant");
        assertThat(methods[0].getParameterCount()).isEqualTo(2);
    }
}
