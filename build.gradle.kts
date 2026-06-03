import com.codecore.gradle.WindowsDirectoryDeleter
import org.gradle.api.tasks.testing.Test

// Runs before Gradle's test output cleanup (which fails if output.bin is locked on Windows).
@Suppress("DEPRECATION")
if (WindowsDirectoryDeleter.isWindows()) {
    gradle.taskGraph.beforeTask {
        if (this is Test) {
            WindowsDirectoryDeleter.deleteRecursively(
                project.layout.buildDirectory.dir("test-results/${name}").get().asFile
            )
        }
    }
}
