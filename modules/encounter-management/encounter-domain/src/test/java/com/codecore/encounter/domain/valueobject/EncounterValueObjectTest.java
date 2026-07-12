package com.codecore.encounter.domain.valueobject;

import com.codecore.encounter.domain.exception.InvalidDomainValueException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncounterValueObjectTest {

    @Test
    void encounterIdShouldSupportEqualityAndGeneration() {
        UUID uuid = UUID.randomUUID();
        EncounterId a = new EncounterId(uuid);
        EncounterId b = new EncounterId(uuid.toString());

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.asString()).isEqualTo(uuid.toString());
        assertThat(EncounterId.generate()).isNotEqualTo(a);
    }

    @Test
    void tenantIdShouldSupportEquality() {
        UUID uuid = UUID.randomUUID();
        assertThat(new TenantId(uuid)).isEqualTo(new TenantId(uuid.toString()));
    }

    @Test
    void referenceIdsShouldWrapUuid() {
        UUID uuid = UUID.randomUUID();

        assertThat(PatientId.of(uuid).value()).isEqualTo(uuid);
        assertThat(StaffAssignmentId.of(uuid).value()).isEqualTo(uuid);
        assertThat(OrganizationId.of(uuid).value()).isEqualTo(uuid);
        assertThat(OfficeId.of(uuid).value()).isEqualTo(uuid);
        assertThat(AppointmentId.of(uuid).value()).isEqualTo(uuid);
    }

    @Test
    void timeBoundsShouldOpenWithoutEndedAt() {
        Instant start = Instant.parse("2026-07-12T14:00:00Z");

        EncounterTimeBounds bounds = EncounterTimeBounds.open(start);

        assertThat(bounds.startedAt()).isEqualTo(start);
        assertThat(bounds.endedAt()).isEmpty();
    }

    @Test
    void timeBoundsShouldAcceptEndedAtEqualToStartedAt() {
        Instant instant = Instant.parse("2026-07-12T14:00:00Z");

        EncounterTimeBounds bounds = EncounterTimeBounds.of(instant, instant);

        assertThat(bounds.startedAt()).isEqualTo(instant);
        assertThat(bounds.endedAt()).contains(instant);
    }

    @Test
    void timeBoundsShouldRejectEndedAtBeforeStartedAt() {
        Instant start = Instant.parse("2026-07-12T14:00:00Z");

        assertThatThrownBy(() -> EncounterTimeBounds.of(start, start.minusSeconds(1)))
                .isInstanceOf(InvalidDomainValueException.class)
                .hasMessageContaining("endedAt");
    }

    @Test
    void timeBoundsWithEndedAtShouldValidateAgainstStartedAt() {
        Instant start = Instant.parse("2026-07-12T14:00:00Z");
        EncounterTimeBounds open = EncounterTimeBounds.open(start);

        assertThatThrownBy(() -> open.withEndedAt(start.minusSeconds(1)))
                .isInstanceOf(InvalidDomainValueException.class);
    }

    @Test
    void statusShouldExposeFrozenLifecycle() {
        assertThat(EncounterStatus.values()).containsExactly(
                EncounterStatus.IN_PROGRESS,
                EncounterStatus.CANCELLED,
                EncounterStatus.COMPLETED
        );
    }
}
