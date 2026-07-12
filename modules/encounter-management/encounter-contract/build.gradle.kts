plugins {
    id("codecore.spring-boot-library")
    `java-library`
}

dependencies {
    // Consumers depend on this module for EncounterId + EncounterReferencePort (ADR-013).
    api(projects.modules.encounterManagement.encounterDomain)
    api(libs.reactor.core)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
