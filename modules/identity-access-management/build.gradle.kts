plugins {
    id("codecore.spring-boot-library")
}

dependencies {

    // Reactive outbound ports (application layer only — domain stays framework-free)
    implementation(libs.reactor.core)

    implementation(projects.shared.sharedKernel)

    // Outbound persistence adapters (R2DBC — infrastructure only)
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")

    implementation(projects.platform.platformR2dbc)

    implementation("org.springframework.security:spring-security-crypto")

    testImplementation(projects.shared.sharedTest)
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    testImplementation(libs.reactor.test)

    testImplementation(libs.flyway.core)
    testImplementation(libs.flyway.database.postgresql)
    testImplementation(libs.postgresql)
    testImplementation(libs.testcontainers.postgresql)
}

tasks.processTestResources {
    from(rootProject.file("apps/codecore-api/src/main/resources/db/migration")) {
        into("db/migration")
    }
}
