plugins {
    id("codecore.spring-boot-library")
}

dependencies {
    implementation(projects.modules.organizationManagement.organizationDomain)
    implementation(libs.reactor.core)
}
