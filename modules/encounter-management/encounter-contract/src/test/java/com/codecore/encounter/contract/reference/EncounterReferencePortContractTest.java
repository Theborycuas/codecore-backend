package com.codecore.encounter.contract.reference;

import com.codecore.encounter.domain.valueobject.EncounterId;
import com.codecore.encounter.domain.valueobject.EncounterStatus;
import com.codecore.encounter.domain.valueobject.PatientId;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncounterReferencePortContractTest {

    @Test
    void shouldExposeExistsInProgressAndFindLinkable() {
        Method[] methods = EncounterReferencePort.class.getDeclaredMethods();
        Set<String> names = Arrays.stream(methods).map(Method::getName).collect(Collectors.toSet());
        assertThat(names).containsExactlyInAnyOrder(
                "existsInProgressByIdAndTenant",
                "findLinkableByIdAndTenant"
        );
        assertThat(methods).allMatch(method -> Modifier.isPublic(method.getModifiers()));
        assertThat(methods).allMatch(method -> method.getParameterCount() == 2);
    }

    @Test
    void referenceViewShouldExposeOnlyLinkableFields() {
        Method[] methods = EncounterReferenceView.class.getDeclaredMethods();
        assertThat(methods)
                .extracting(Method::getName)
                .contains("encounterId", "patientId", "status", "isLinkableForClinicalDocs");
        assertThat(methods)
                .extracting(Method::getName)
                .doesNotContain("organizationId", "startedAt", "endedAt", "appointmentId", "save", "cancel", "complete");
    }

    @Test
    void referenceViewShouldRejectCancelledStatus() {
        assertThatThrownBy(() -> new EncounterReferenceView(
                EncounterId.generate(),
                PatientId.of(UUID.randomUUID()),
                EncounterStatus.CANCELLED
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
