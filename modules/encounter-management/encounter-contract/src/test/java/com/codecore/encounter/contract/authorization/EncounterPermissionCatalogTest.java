package com.codecore.encounter.contract.authorization;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EncounterPermissionCatalogTest {

    @Test
    void shouldExposeFourEncounterLifecyclePermissions() {
        assertThat(EncounterPermissionCatalog.ALL).hasSize(4);
        assertThat(EncounterPermissionCatalog.ENCOUNTER_READ_ONLY)
                .containsExactly(EncounterPermissionCatalog.ENCOUNTER_READ);
    }

    @Test
    void shouldUseCancelNotDeleteAndMapCompleteToUpdate() {
        assertThat(EncounterPermissionCatalog.ALL)
                .contains(EncounterPermissionCatalog.ENCOUNTER_CANCEL)
                .contains(EncounterPermissionCatalog.ENCOUNTER_UPDATE)
                .doesNotContain("encounter:delete")
                .doesNotContain("encounter:complete")
                .doesNotContain("encounter:restore");
    }

    @Test
    void shouldRemainVerticalAgnostic() {
        assertThat(EncounterPermissionCatalog.ALL).noneMatch(code ->
                code.contains("dental")
                        || code.contains("vet")
                        || code.contains("hospital")
                        || code.contains("surgery")
                        || code.contains("hygiene")
                        || code.contains("soap")
                        || code.contains("odontogram")
                        || code.contains("note")
        );
    }
}
