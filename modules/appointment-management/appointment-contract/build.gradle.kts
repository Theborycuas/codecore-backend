plugins {
    id("codecore.spring-boot-library")
    `java-library`
}

dependencies {
    // Future consumers depend on this module for AppointmentId (and later AppointmentReferencePort).
    api(projects.modules.appointmentManagement.appointmentDomain)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
