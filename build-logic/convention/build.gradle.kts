plugins {
    `kotlin-dsl`
}

group = "com.codecore.buildlogic"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {

    implementation("org.springframework.boot:spring-boot-gradle-plugin:3.5.4")

    implementation("io.spring.dependency-management:io.spring.dependency-management.gradle.plugin:1.1.7")

    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.21")
}