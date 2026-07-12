plugins {
    id("codecore.spring-boot-library")
    `java-library`
}

dependencies {
    // Consumers depend on this module for EncounterId (and later EncounterReferencePort).
    api(projects.modules.encounterManagement.encounterDomain)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
