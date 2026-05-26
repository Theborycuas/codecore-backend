plugins {
    id("codecore.java-conventions")
}

dependencies {

    implementation(projects.shared.sharedKernel)

    implementation(libs.postgresql)

    implementation(libs.flyway.core)

    implementation(libs.flyway.database.postgresql)

    testImplementation(libs.junit.jupiter)
}