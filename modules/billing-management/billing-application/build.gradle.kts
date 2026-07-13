plugins {
    id("codecore.spring-boot-library")
}

dependencies {
    implementation(projects.modules.billingManagement.billingDomain)
    implementation(projects.modules.organizationManagement.organizationContract)
    implementation(projects.modules.patientManagement.patientContract)
    implementation(projects.modules.encounterManagement.encounterContract)
    implementation(projects.modules.inventoryManagement.inventoryContract)
    implementation(libs.reactor.core)
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(libs.reactor.test)
    testImplementation("org.mockito:mockito-junit-jupiter")
}
