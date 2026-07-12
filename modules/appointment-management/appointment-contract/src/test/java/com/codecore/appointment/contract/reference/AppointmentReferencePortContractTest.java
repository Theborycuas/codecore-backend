package com.codecore.appointment.contract.reference;

import com.codecore.appointment.domain.valueobject.AppointmentId;
import com.codecore.appointment.domain.valueobject.AppointmentStatus;
import com.codecore.appointment.domain.valueobject.PatientId;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentReferencePortContractTest {

    @Test
    void shouldExposeExistsScheduledAndFindLinkable() {
        Method[] methods = AppointmentReferencePort.class.getDeclaredMethods();
        Set<String> names = Arrays.stream(methods).map(Method::getName).collect(Collectors.toSet());
        assertThat(names).containsExactlyInAnyOrder(
                "existsScheduledByIdAndTenant",
                "findLinkableByIdAndTenant"
        );
        assertThat(methods).allMatch(method -> Modifier.isPublic(method.getModifiers()));
        assertThat(methods).allMatch(method -> method.getParameterCount() == 2);
    }

    @Test
    void referenceViewShouldExposeOnlyLinkableFields() {
        Method[] methods = AppointmentReferenceView.class.getDeclaredMethods();
        assertThat(methods)
                .extracting(Method::getName)
                .contains("appointmentId", "patientId", "status", "isLinkableForEncounter");
        assertThat(methods)
                .extracting(Method::getName)
                .doesNotContain("organizationId", "startsAt", "endsAt", "save", "cancel", "complete");
    }

    @Test
    void referenceViewShouldRejectCancelledStatus() {
        assertThatThrownBy(() -> new AppointmentReferenceView(
                AppointmentId.generate(),
                PatientId.of(UUID.randomUUID()),
                AppointmentStatus.CANCELLED
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
