package com.codecore.patient.contract.authorization;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PatientPermissionCatalogTest {

    @Test
    void shouldExposeFourPatientRegistryPermissions() {
        assertThat(PatientPermissionCatalog.ALL).hasSize(4);
        assertThat(PatientPermissionCatalog.PATIENT_READ_ONLY).containsExactly(PatientPermissionCatalog.PATIENT_READ);
    }

    @Test
    void shouldUseArchiveNotDelete() {
        assertThat(PatientPermissionCatalog.ALL)
                .contains(PatientPermissionCatalog.PATIENT_ARCHIVE)
                .doesNotContain("patient:delete")
                .doesNotContain("patient:merge")
                .doesNotContain("patient:anonymize");
    }

    @Test
    void shouldRemainVerticalAgnostic() {
        assertThat(PatientPermissionCatalog.ALL).noneMatch(code ->
                code.contains("dental")
                        || code.contains("vet")
                        || code.contains("hospital")
                        || code.contains("odontogram")
                        || code.contains("appointment")
        );
    }
}
