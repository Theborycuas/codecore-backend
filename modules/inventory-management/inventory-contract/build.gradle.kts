plugins {
    id("codecore.spring-boot-library")
    `java-library`
}

dependencies {
    // Consumers depend on this module for ItemId + ItemReferencePort (ADR-013 / ADR-016).
    api(projects.modules.inventoryManagement.inventoryDomain)
    api(libs.reactor.core)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
