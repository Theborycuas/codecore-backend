plugins {
    java
    jacoco
}

group = "com.codecore"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

tasks.withType<JavaCompile>().configureEach {

    options.encoding = "UTF-8"

    options.compilerArgs.addAll(
        listOf(
            "-parameters"
        )
    )
}

tasks.withType<Test>().configureEach {

    useJUnitPlatform()

    testLogging {
        events(
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED,
            TestLogEvent.FAILED
        )
    }
}

jacoco {
    toolVersion = "0.8.13"
}