plugins {
    id("codecore.spring-boot-library")
    `java-library`
}

dependencies {
    // Consumers depend on this module for AppointmentId + AppointmentReferencePort (ADR-013).
    api(projects.modules.appointmentManagement.appointmentDomain)
    api(libs.reactor.core)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
