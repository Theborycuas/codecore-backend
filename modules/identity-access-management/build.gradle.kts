plugins {
    id("codecore.spring-boot-library")
}

dependencies {

    // Reactive outbound ports (application layer only — domain stays framework-free)
    implementation(libs.reactor.core)

    implementation(projects.shared.sharedKernel)

    testImplementation(projects.shared.sharedTest)
}
