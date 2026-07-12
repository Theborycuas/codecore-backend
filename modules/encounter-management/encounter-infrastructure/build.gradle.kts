plugins {
    id("codecore.spring-boot-library")
}

dependencies {
    implementation(projects.modules.encounterManagement.encounterDomain)
    implementation(projects.modules.encounterManagement.encounterApplication)
    implementation(projects.modules.encounterManagement.encounterContract)
}
