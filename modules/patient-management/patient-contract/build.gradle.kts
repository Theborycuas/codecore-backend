plugins {
    id("codecore.spring-boot-library")
    `java-library`
}

dependencies {
    // Future consumers depend on this module for PatientId (and later PatientReferencePort).
    api(projects.modules.patientManagement.patientDomain)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
