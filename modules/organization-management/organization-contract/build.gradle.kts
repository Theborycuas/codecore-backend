plugins {
    id("codecore.spring-boot-library")
    `java-library`
}

dependencies {
    api(projects.modules.organizationManagement.organizationDomain)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
