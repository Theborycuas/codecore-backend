plugins {
    id("codecore.java-conventions")
}

dependencies {

    implementation(projects.shared.sharedKernel)

    implementation(libs.junit.jupiter)

    implementation(libs.reactor.test)

    implementation(libs.testcontainers)

    implementation(libs.testcontainers.postgresql)

    testImplementation(libs.junit.jupiter)
}