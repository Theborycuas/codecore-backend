package com.codecore.organization.domain.valueobject;

import com.codecore.organization.domain.exception.InvalidDomainValueException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrganizationNameTest {

    @Test
    void shouldAcceptValidName() {
        OrganizationName name = OrganizationName.of("  Dental Norte  ");

        assertThat(name.value()).isEqualTo("Dental Norte");
    }

    @Test
    void shouldRejectBlankName() {
        assertThatThrownBy(() -> OrganizationName.of("   "))
                .isInstanceOf(InvalidDomainValueException.class);
    }

    @Test
    void shouldRejectNullName() {
        assertThatThrownBy(() -> OrganizationName.of(null))
                .isInstanceOf(NullPointerException.class);
    }
}
