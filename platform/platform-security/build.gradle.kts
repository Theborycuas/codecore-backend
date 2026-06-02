plugins {
    id("codecore.spring-boot-library")
}

dependencies {

    implementation(projects.shared.sharedKernel)

    implementation(projects.platform.platformWebflux)

    implementation(libs.spring.boot.starter.security)

    implementation(libs.reactor.core)

    testImplementation(libs.reactor.test)

    testImplementation(libs.junit.jupiter)
}