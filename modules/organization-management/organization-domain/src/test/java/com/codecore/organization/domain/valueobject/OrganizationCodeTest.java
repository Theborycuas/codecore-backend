package com.codecore.organization.domain.valueobject;

import com.codecore.organization.domain.exception.InvalidDomainValueException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrganizationCodeTest {

    @Test
    void shouldAcceptValidCodes() {
        assertThat(OrganizationCode.of("DENTAL_NORTE").value()).isEqualTo("DENTAL_NORTE");
        assertThat(OrganizationCode.of("CARDIOLOGIA").value()).isEqualTo("CARDIOLOGIA");
        assertThat(OrganizationCode.of("EMERGENCIAS").value()).isEqualTo("EMERGENCIAS");
    }

    @Test
    void shouldNormalizeCode() {
        assertThat(OrganizationCode.of(" dental-norte ").value()).isEqualTo("DENTAL_NORTE");
        assertThat(OrganizationCode.of("dental sur").value()).isEqualTo("DENTAL_SUR");
    }

    @Test
    void shouldRejectInvalidCodes() {
        assertThatThrownBy(() -> OrganizationCode.of(""))
                .isInstanceOf(InvalidDomainValueException.class);
        assertThatThrownBy(() -> OrganizationCode.of("   "))
                .isInstanceOf(InvalidDomainValueException.class);
        assertThatThrownBy(() -> OrganizationCode.of("a"))
                .isInstanceOf(InvalidDomainValueException.class);
        assertThatThrownBy(() -> OrganizationCode.of("1INVALID"))
                .isInstanceOf(InvalidDomainValueException.class);
        assertThatThrownBy(() -> OrganizationCode.of("_LEADING"))
                .isInstanceOf(InvalidDomainValueException.class);
        assertThatThrownBy(() -> OrganizationCode.of("special!"))
                .isInstanceOf(InvalidDomainValueException.class);
    }

    @Test
    void shouldRejectNullCode() {
        assertThatThrownBy(() -> OrganizationCode.of(null))
                .isInstanceOf(NullPointerException.class);
    }
}
