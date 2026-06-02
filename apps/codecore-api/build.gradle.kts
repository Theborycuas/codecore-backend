plugins {
    id("codecore.spring-boot-application")
}

dependencies {

    // =========================
    // Shared
    // =========================
    implementation(projects.shared.sharedKernel)
    implementation(projects.shared.sharedEvents)

    // =========================
    // Platform
    // =========================
    implementation(projects.platform.platformWebflux)
    implementation(projects.platform.platformSecurity)
    implementation(projects.platform.platformR2dbc)
    implementation(projects.platform.platformPostgres)
    implementation(projects.platform.platformRedis)
    implementation(projects.platform.platformKafka)
    implementation(projects.platform.platformTelemetry)
    

    // =========================
    // Modules
    // =========================
    implementation(projects.modules.identityAccessManagement)

    // =========================
    // Testing
    // =========================
    testImplementation(projects.shared.sharedTest)

    // Flyway usa JDBC (spring.flyway.url); R2DBC es solo para acceso reactivo en runtime.
    // En Spring Boot 3.x no existe spring-boot-starter-flyway (solo desde Boot 4).
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
}