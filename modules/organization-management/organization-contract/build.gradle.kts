plugins {
    id("codecore.java-conventions")
    `java-library`
}

dependencies {
    api(projects.modules.organizationManagement.organizationDomain)
}
