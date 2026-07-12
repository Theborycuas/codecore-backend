plugins {
    id("codecore.spring-boot-library")
}

dependencies {
    implementation(projects.modules.appointmentManagement.appointmentDomain)
    implementation(projects.modules.appointmentManagement.appointmentApplication)
    implementation(projects.modules.appointmentManagement.appointmentContract)
}
