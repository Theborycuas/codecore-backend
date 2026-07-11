plugins {
    id("codecore.spring-boot-library")
}

dependencies {
    implementation(projects.modules.patientManagement.patientDomain)
    implementation(libs.reactor.core)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(libs.reactor.test)
}
