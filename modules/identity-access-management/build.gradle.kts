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

    implementation(projects.platform.platformSecurity)

    implementation(libs.spring.boot.starter.security)

    implementation(libs.spring.boot.starter.webflux)

    implementation(libs.spring.boot.starter.validation)

    implementation(libs.spring.security.crypto)

    // JJWT (JJWT 0.12.x — API must be on compile classpath for JwtTokenProvider)
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    implementation("io.jsonwebtoken:jjwt-impl:0.12.6")
    implementation("io.jsonwebtoken:jjwt-jackson:0.12.6")

    testImplementation(projects.shared.sharedTest)
    testImplementation(projects.platform.platformSecurity)
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

tasks.test {
    // Avoid Windows file locks on reused Gradle binary test-results directories.
    binaryResultsDirectory.set(
            layout.buildDirectory.dir("tmp/test-binary-${System.currentTimeMillis()}")
    )
}
