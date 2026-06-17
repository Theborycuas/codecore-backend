package com.codecore.iam.domain.model.tenant;

import com.codecore.iam.domain.exception.InvalidDomainValueException;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.domain.valueobject.TenantName;
import com.codecore.iam.domain.valueobject.TenantStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantTest {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");

    @Test
    void shouldCreateActiveTenant() {
        TenantId tenantId = TenantId.generate();
        TenantName name = TenantName.of("Acme Corp");

        Tenant tenant = Tenant.create(tenantId, name, NOW);

        assertThat(tenant.id()).isEqualTo(tenantId);
        assertThat(tenant.name()).isEqualTo(name);
        assertThat(tenant.status()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(tenant.createdAt()).isEqualTo(NOW);
        assertThat(tenant.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldRejectBlankTenantName() {
        assertThatThrownBy(() -> TenantName.of("   "))
                .isInstanceOf(InvalidDomainValueException.class);
    }

    @Test
    void shouldSuspendDisableAndActivate() {
        Tenant tenant = Tenant.create(
                TenantId.generate(),
                TenantName.of("Lifecycle Tenant"),
                NOW
        );

        tenant.suspend();
        assertThat(tenant.status()).isEqualTo(TenantStatus.SUSPENDED);

        tenant.disable();
        assertThat(tenant.status()).isEqualTo(TenantStatus.DISABLED);

        tenant.activate();
        assertThat(tenant.status()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(tenant.updatedAt()).isAfter(NOW);
    }

    @Test
    void shouldRenameTenant() {
        Tenant tenant = Tenant.create(
                TenantId.generate(),
                TenantName.of("Original Name"),
                NOW
        );

        tenant.rename(TenantName.of("Renamed Tenant"));

        assertThat(tenant.name().value()).isEqualTo("Renamed Tenant");
        assertThat(tenant.updatedAt()).isAfter(NOW);
    }
}
