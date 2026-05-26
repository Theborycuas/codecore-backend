plugins {
    id("codecore.java-conventions")
}

dependencies {

    implementation(projects.shared.sharedKernel)

    implementation(libs.spring.boot.starter.actuator)

    implementation(libs.micrometer.core)

    implementation(libs.opentelemetry.api)

    implementation(libs.reactor.core)

    testImplementation(libs.junit.jupiter)
}