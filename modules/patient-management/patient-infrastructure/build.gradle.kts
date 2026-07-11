plugins {
    id("codecore.spring-boot-library")
}

dependencies {
    implementation(projects.modules.patientManagement.patientDomain)
    implementation(projects.modules.patientManagement.patientApplication)
    implementation(projects.modules.patientManagement.patientContract)
    implementation(projects.modules.organizationManagement.organizationContract)
    implementation(projects.modules.identityAccessManagement)

    implementation(libs.reactor.core)
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation(projects.platform.platformR2dbc)
    implementation(projects.platform.platformWebflux)
    implementation(libs.springdoc.openapi.starter.webflux.api)

    testImplementation(projects.shared.sharedTest)
    testImplementation(projects.modules.identityAccessManagement)
    testImplementation(projects.modules.organizationManagement.organizationInfrastructure)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
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
