plugins {
    id("codecore.java-conventions")
}

dependencies {

    implementation(projects.shared.sharedKernel)

    implementation(projects.shared.sharedEvents)

    implementation(libs.spring.kafka)

    implementation(libs.reactor.core)

    testImplementation(libs.reactor.test)

    testImplementation(libs.junit.jupiter)

    testImplementation(libs.testcontainers)
}