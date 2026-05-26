plugins {
    id("codecore.java-conventions")

    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {

    implementation("org.springframework.boot:spring-boot-starter")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

tasks.named<Jar>("jar") {
    enabled = true
}