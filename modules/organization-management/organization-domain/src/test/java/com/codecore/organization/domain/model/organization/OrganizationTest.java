package com.codecore.organization.domain.model.organization;

import com.codecore.organization.domain.exception.InvalidDomainValueException;
import com.codecore.organization.domain.exception.InvalidOrganizationStateException;
import com.codecore.organization.domain.valueobject.OrganizationCode;
import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.organization.domain.valueobject.OrganizationName;
import com.codecore.organization.domain.valueobject.OrganizationStatus;
import com.codecore.organization.domain.valueobject.TenantId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrganizationTest {

    private static final Instant NOW = Instant.parse("2026-06-22T12:00:00Z");

    @Test
    void shouldCreateValidOrganization() {
        OrganizationId id = OrganizationId.generate();
        TenantId tenantId = TenantId.generate();
        OrganizationCode code = OrganizationCode.of("dental_norte");
        OrganizationName name = OrganizationName.of("Dental Norte");

        Organization organization = Organization.create(id, tenantId, code, name, NOW);

        assertThat(organization.id()).isEqualTo(id);
        assertThat(organization.tenantId()).isEqualTo(tenantId);
        assertThat(organization.code().value()).isEqualTo("DENTAL_NORTE");
        assertThat(organization.name()).isEqualTo(name);
        assertThat(organization.status()).isEqualTo(OrganizationStatus.ACTIVE);
        assertThat(organization.createdAt()).isEqualTo(NOW);
        assertThat(organization.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldRenameOrganization() {
        Organization organization = activeOrganization();

        organization.rename(OrganizationName.of("Dental Sur Renovado"));

        assertThat(organization.name().value()).isEqualTo("Dental Sur Renovado");
        assertThat(organization.updatedAt()).isAfter(NOW);
    }

    @Test
    void shouldArchiveOrganization() {
        Organization organization = activeOrganization();

        organization.archive();

        assertThat(organization.status()).isEqualTo(OrganizationStatus.ARCHIVED);
        assertThat(organization.updatedAt()).isAfter(NOW);
    }

    @Test
    void shouldActivateArchivedOrganization() {
        Organization organization = activeOrganization();
        organization.archive();

        organization.activate();

        assertThat(organization.status()).isEqualTo(OrganizationStatus.ACTIVE);
        assertThat(organization.updatedAt()).isAfter(NOW);
    }

    @Test
    void shouldRejectArchiveWhenAlreadyArchived() {
        Organization organization = activeOrganization();
        organization.archive();

        assertThatThrownBy(organization::archive)
                .isInstanceOf(InvalidOrganizationStateException.class)
                .hasMessageContaining("already archived");
    }

    @Test
    void shouldRejectActivateWhenAlreadyActive() {
        Organization organization = activeOrganization();

        assertThatThrownBy(organization::activate)
                .isInstanceOf(InvalidOrganizationStateException.class)
                .hasMessageContaining("already active");
    }

    @Test
    void shouldRequireTenantId() {
        assertThatThrownBy(() -> Organization.create(
                OrganizationId.generate(),
                null,
                OrganizationCode.of("CARDIOLOGIA"),
                OrganizationName.of("Cardiología"),
                NOW
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("tenantId");
    }

    @Test
    void shouldKeepTenantIdImmutableAfterCreation() {
        Organization organization = activeOrganization();
        TenantId originalTenantId = organization.tenantId();

        organization.rename(OrganizationName.of("Renamed"));
        organization.archive();
        organization.activate();

        assertThat(organization.tenantId()).isEqualTo(originalTenantId);
    }

    @Test
    void shouldReconstituteOrganization() {
        OrganizationId id = OrganizationId.generate();
        TenantId tenantId = TenantId.generate();
        OrganizationCode code = OrganizationCode.of("EMERGENCIAS");
        OrganizationName name = OrganizationName.of("Emergencias");
        Instant createdAt = NOW.minusSeconds(3600);
        Instant updatedAt = NOW.minusSeconds(60);

        Organization organization = Organization.reconstitute(
                id,
                tenantId,
                code,
                name,
                OrganizationStatus.ARCHIVED,
                createdAt,
                updatedAt
        );

        assertThat(organization.id()).isEqualTo(id);
        assertThat(organization.tenantId()).isEqualTo(tenantId);
        assertThat(organization.code()).isEqualTo(code);
        assertThat(organization.name()).isEqualTo(name);
        assertThat(organization.status()).isEqualTo(OrganizationStatus.ARCHIVED);
        assertThat(organization.createdAt()).isEqualTo(createdAt);
        assertThat(organization.updatedAt()).isEqualTo(updatedAt);
    }

    private static Organization activeOrganization() {
        return Organization.create(
                OrganizationId.generate(),
                TenantId.generate(),
                OrganizationCode.of("DENTAL_SUR"),
                OrganizationName.of("Dental Sur"),
                NOW
        );
    }
}
