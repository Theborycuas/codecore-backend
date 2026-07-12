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
    implementation(projects.modules.organizationManagement.organizationInfrastructure)
    implementation(projects.modules.patientManagement.patientInfrastructure)
    implementation(projects.modules.appointmentManagement.appointmentInfrastructure)
    implementation(projects.modules.encounterManagement.encounterInfrastructure)

    implementation(libs.springdoc.openapi.starter.webflux.ui)

    // =========================
    // Testing
    // =========================
    testImplementation(projects.shared.sharedTest)

    // Flyway usa JDBC (spring.flyway.url); R2DBC es solo para acceso reactivo en runtime.
    // En Spring Boot 3.x no existe spring-boot-starter-flyway (solo desde Boot 4).
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
}

/**
 * Loads codecore-backend/.env into the bootRun process (Spring Boot does not read .env by itself).
 */
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    val dotEnv = rootProject.layout.projectDirectory.file(".env").asFile
    if (dotEnv.exists()) {
        dotEnv.readLines()
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { line ->
                val separator = line.indexOf('=')
                if (separator <= 0) {
                    null
                } else {
                    line.substring(0, separator).trim() to line.substring(separator + 1).trim()
                }
            }
            .forEach { (key, value) -> environment(key, value) }
    }
}