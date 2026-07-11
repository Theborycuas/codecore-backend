plugins {
    id("codecore.spring-boot-library")
}

dependencies {
    implementation(projects.modules.patientManagement.patientDomain)
    implementation(projects.modules.patientManagement.patientApplication)
    implementation(projects.modules.patientManagement.patientContract)
}
