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
    // Testing
    // =========================
    testImplementation(projects.shared.sharedTest)
}