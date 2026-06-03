package com.codecore.gradle

import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.DosFileAttributeView

/**
 * Best-effort recursive delete for Gradle output dirs on Windows (read-only / file locks).
 */
object WindowsDirectoryDeleter {

    fun isWindows(): Boolean =
        System.getProperty("os.name").orEmpty().lowercase().contains("win")

    fun deleteRecursively(root: File) {
        if (!root.exists()) {
            return
        }
        repeat(5) { attempt ->
            if (deleteOnce(root) || !root.exists()) {
                return
            }
            if (attempt < 4) {
                Thread.sleep(200L * (attempt + 1))
            }
        }
    }

    private fun deleteOnce(root: File): Boolean {
        if (!root.exists()) {
            return true
        }
        var failed = false
        root.walkBottomUp().forEach { file ->
            if (!file.exists()) {
                return@forEach
            }
            clearReadOnly(file)
            try {
                if (!Files.deleteIfExists(file.toPath()) && file.exists()) {
                    failed = true
                }
            } catch (_: Exception) {
                file.setWritable(true)
                if (!file.delete() && file.exists()) {
                    failed = true
                }
            }
        }
        return !failed && !root.exists()
    }

    private fun clearReadOnly(file: File) {
        try {
            val view = Files.getFileAttributeView(file.toPath(), DosFileAttributeView::class.java)
            view?.setReadOnly(false)
        } catch (_: Exception) {
            file.setWritable(true)
        }
    }
}
