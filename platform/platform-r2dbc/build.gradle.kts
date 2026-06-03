plugins {
    id("codecore.spring-boot-library")
}

dependencies {

    implementation(projects.shared.sharedKernel)

    implementation(projects.platform.platformPostgres)

    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-r2dbc")
    implementation("org.springframework:spring-tx")

    implementation(libs.r2dbc.postgresql)
    implementation(libs.reactor.core)

    testImplementation(libs.reactor.test)

    testImplementation(libs.junit.jupiter)
}