plugins {
    id("codecore.java-conventions")
}

dependencies {

    implementation(projects.shared.sharedKernel)

    implementation(projects.platform.platformPostgres)

    implementation(libs.r2dbc.postgresql)

    implementation(libs.reactor.core)

    testImplementation(libs.reactor.test)

    testImplementation(libs.junit.jupiter)
}