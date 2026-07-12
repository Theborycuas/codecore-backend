package com.codecore.appointment.domain.valueobject;

import com.codecore.appointment.domain.exception.InvalidDomainValueException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentValueObjectTest {

    @Test
    void appointmentIdShouldSupportEqualityAndGeneration() {
        UUID uuid = UUID.randomUUID();
        AppointmentId a = new AppointmentId(uuid);
        AppointmentId b = new AppointmentId(uuid.toString());

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.asString()).isEqualTo(uuid.toString());
        assertThat(AppointmentId.generate()).isNotEqualTo(a);
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
    }

    @Test
    void timeWindowShouldAcceptValidRange() {
        Instant start = Instant.parse("2026-07-12T14:00:00Z");
        Instant end = Instant.parse("2026-07-12T15:00:00Z");

        AppointmentTimeWindow window = AppointmentTimeWindow.of(start, end);

        assertThat(window.startsAt()).isEqualTo(start);
        assertThat(window.endsAt()).isEqualTo(end);
        assertThat(window).isEqualTo(AppointmentTimeWindow.of(start, end));
    }

    @Test
    void timeWindowShouldRejectEndsAtNotAfterStartsAt() {
        Instant instant = Instant.parse("2026-07-12T14:00:00Z");

        assertThatThrownBy(() -> AppointmentTimeWindow.of(instant, instant))
                .isInstanceOf(InvalidDomainValueException.class)
                .hasMessageContaining("endsAt");

        assertThatThrownBy(() -> AppointmentTimeWindow.of(instant, instant.minusSeconds(1)))
                .isInstanceOf(InvalidDomainValueException.class);
    }

    @Test
    void timeWindowShouldRejectNullBounds() {
        Instant instant = Instant.parse("2026-07-12T14:00:00Z");

        assertThatThrownBy(() -> AppointmentTimeWindow.of(null, instant))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("startsAt");

        assertThatThrownBy(() -> AppointmentTimeWindow.of(instant, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("endsAt");
    }

    @Test
    void statusShouldExposeFrozenLifecycle() {
        assertThat(AppointmentStatus.values()).containsExactly(
                AppointmentStatus.SCHEDULED,
                AppointmentStatus.CANCELLED,
                AppointmentStatus.COMPLETED
        );
    }
}
