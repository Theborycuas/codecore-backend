plugins {
    id("codecore.spring-boot-library")
}

dependencies {
    implementation(projects.modules.accessManagement.accessDomain)
    implementation(projects.modules.accessManagement.accessContract)
    implementation(projects.modules.auditManagement.auditContract)
    implementation(projects.modules.identityAccessManagement)
    implementation(libs.reactor.core)
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(libs.reactor.test)
    testImplementation("org.mockito:mockito-junit-jupiter")
}
