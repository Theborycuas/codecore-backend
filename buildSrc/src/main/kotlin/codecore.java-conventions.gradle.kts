import com.codecore.gradle.WindowsDirectoryDeleter
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestLogEvent

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

    if (WindowsDirectoryDeleter.isWindows()) {
        doFirst {
            WindowsDirectoryDeleter.deleteRecursively(
                layout.buildDirectory.dir("test-results/${name}").get().asFile
            )
        }
    }
}

jacoco {
    toolVersion = "0.8.13"
}