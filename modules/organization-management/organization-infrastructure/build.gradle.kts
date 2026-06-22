plugins {
    id("codecore.spring-boot-library")
}

dependencies {
    implementation(projects.modules.organizationManagement.organizationDomain)
    implementation(projects.modules.organizationManagement.organizationApplication)

    implementation(libs.reactor.core)
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation(projects.platform.platformR2dbc)

    testImplementation(projects.shared.sharedTest)
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
