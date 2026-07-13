plugins {
    id("codecore.spring-boot-library")
    `java-library`
}

dependencies {
    // Consumers depend on this module for InvitationId + authorization contract (ADR-013 / ADR-019).
    api(projects.modules.accessManagement.accessDomain)
    api(libs.reactor.core)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
