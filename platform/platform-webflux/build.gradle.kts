plugins {
    id("codecore.java-conventions")
}

dependencies {

    implementation(projects.shared.sharedKernel)

    implementation(libs.spring.boot.starter.webflux)

    implementation(libs.reactor.core)

    testImplementation(libs.reactor.test)

    testImplementation(libs.junit.jupiter)
}