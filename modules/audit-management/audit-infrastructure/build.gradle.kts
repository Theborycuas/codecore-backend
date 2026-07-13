plugins {
    id("codecore.spring-boot-library")
}

dependencies {
    implementation(projects.modules.auditManagement.auditDomain)
    implementation(projects.modules.auditManagement.auditApplication)
    implementation(projects.modules.auditManagement.auditContract)
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
    testImplementation(projects.modules.accessManagement.accessDomain)
    testImplementation(projects.modules.accessManagement.accessApplication)
    testImplementation(projects.modules.accessManagement.accessInfrastructure)
    testImplementation(projects.modules.organizationManagement.organizationInfrastructure)
    testImplementation(projects.modules.patientManagement.patientInfrastructure)
    testImplementation(projects.modules.inventoryManagement.inventoryInfrastructure)
    testImplementation(projects.modules.encounterManagement.encounterInfrastructure)
    testImplementation(projects.modules.appointmentManagement.appointmentInfrastructure)
    testImplementation(projects.modules.billingManagement.billingInfrastructure)
    testImplementation(projects.modules.paymentManagement.paymentInfrastructure)
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
