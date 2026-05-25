plugins {
    id("codecore.java-conventions")
}

dependencies {

    implementation(projects.shared.sharedKernel)

    testImplementation(libs.junit.jupiter)
}