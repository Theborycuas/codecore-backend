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

    implementation(projects.platform.platformWebflux)

    implementation(libs.spring.boot.starter.webflux)

    implementation(libs.spring.boot.starter.validation)

    implementation(libs.spring.security.crypto)

    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

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
