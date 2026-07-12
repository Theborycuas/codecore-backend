package com.codecore.appointment.contract.authorization;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentPermissionCatalogTest {

    @Test
    void shouldExposeFourAppointmentLifecyclePermissions() {
        assertThat(AppointmentPermissionCatalog.ALL).hasSize(4);
        assertThat(AppointmentPermissionCatalog.APPOINTMENT_READ_ONLY)
                .containsExactly(AppointmentPermissionCatalog.APPOINTMENT_READ);
    }

    @Test
    void shouldUseCancelNotDeleteAndMapCompleteToUpdate() {
        assertThat(AppointmentPermissionCatalog.ALL)
                .contains(AppointmentPermissionCatalog.APPOINTMENT_CANCEL)
                .contains(AppointmentPermissionCatalog.APPOINTMENT_UPDATE)
                .doesNotContain("appointment:delete")
                .doesNotContain("appointment:complete")
                .doesNotContain("appointment:restore");
    }

    @Test
    void shouldRemainVerticalAgnostic() {
        assertThat(AppointmentPermissionCatalog.ALL).noneMatch(code ->
                code.contains("dental")
                        || code.contains("vet")
                        || code.contains("hospital")
                        || code.contains("chair")
                        || code.contains("surgery")
                        || code.contains("slot")
                        || code.contains("odontogram")
        );
    }
}
