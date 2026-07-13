plugins {
    id("codecore.spring-boot-library")
    `java-library`
}

dependencies {
    // Consumers depend on this module for InvoiceId + authorization contract (ADR-013 / ADR-017).
    api(projects.modules.billingManagement.billingDomain)
    api(libs.reactor.core)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
