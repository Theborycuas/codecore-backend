plugins {
    id("codecore.spring-boot-library")
    `java-library`
}

dependencies {
    // Consumers depend on this module for PatientId, permissions, and PatientReferencePort (ADR-013).
    api(projects.modules.patientManagement.patientDomain)
    api(libs.reactor.core)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
