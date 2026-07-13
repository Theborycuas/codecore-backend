plugins {
    id("codecore.spring-boot-library")
    `java-library`
}

dependencies {
    // Consumers depend on this module for PaymentId + authorization contract (ADR-013 / ADR-018).
    api(projects.modules.paymentManagement.paymentDomain)
    api(libs.reactor.core)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
