package com.codecore.organization.domain.model.office;

import com.codecore.organization.domain.exception.InvalidOfficeStateException;
import com.codecore.organization.domain.valueobject.OfficeCode;
import com.codecore.organization.domain.valueobject.OfficeId;
import com.codecore.organization.domain.valueobject.OfficeName;
import com.codecore.organization.domain.valueobject.OfficeStatus;
import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.organization.domain.valueobject.TenantId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OfficeTest {

    private static final Instant NOW = Instant.parse("2026-06-22T12:00:00Z");

    @Test
    void shouldCreateValidOffice() {
        OfficeId id = OfficeId.generate();
        TenantId tenantId = TenantId.generate();
        OrganizationId organizationId = OrganizationId.generate();
        OfficeCode code = OfficeCode.of("consultorio_1");
        OfficeName name = OfficeName.of("Consultorio 1");

        Office office = Office.create(id, tenantId, organizationId, code, name, NOW);

        assertThat(office.id()).isEqualTo(id);
        assertThat(office.tenantId()).isEqualTo(tenantId);
        assertThat(office.organizationId()).isEqualTo(organizationId);
        assertThat(office.code().value()).isEqualTo("CONSULTORIO_1");
        assertThat(office.status()).isEqualTo(OfficeStatus.ACTIVE);
    }

    @Test
    void shouldArchiveOffice() {
        Office office = activeOffice();
        office.archive();
        assertThat(office.status()).isEqualTo(OfficeStatus.ARCHIVED);
    }

    @Test
    void shouldRejectArchiveWhenAlreadyArchived() {
        Office office = activeOffice();
        office.archive();
        assertThatThrownBy(office::archive)
                .isInstanceOf(InvalidOfficeStateException.class);
    }

    private static Office activeOffice() {
        return Office.create(
                OfficeId.generate(),
                TenantId.generate(),
                OrganizationId.generate(),
                OfficeCode.of("MAIN"),
                OfficeName.of("Main Office"),
                NOW
        );
    }
}
