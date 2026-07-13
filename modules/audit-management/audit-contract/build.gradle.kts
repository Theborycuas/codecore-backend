plugins {
    id("codecore.spring-boot-library")
    `java-library`
}

dependencies {
    // Consumers depend on this module for AuditEntryId + authorization / append contract (ADR-013 / ADR-020).
    api(projects.modules.auditManagement.auditDomain)
    api(libs.reactor.core)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
