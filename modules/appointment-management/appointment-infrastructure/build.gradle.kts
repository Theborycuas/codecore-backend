plugins {
    id("codecore.spring-boot-library")
}

dependencies {
    implementation(projects.modules.appointmentManagement.appointmentDomain)
    implementation(projects.modules.appointmentManagement.appointmentApplication)
    implementation(projects.modules.appointmentManagement.appointmentContract)

    implementation(libs.reactor.core)
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation(projects.platform.platformR2dbc)

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
