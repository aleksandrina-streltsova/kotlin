package org.jetbrains.kotlin.tools.projectWizard.core

typealias Path = java.nio.file.Path

object Paths {
    fun get(first: String, vararg more: String): Path = java.nio.file.Paths.get(first, *more)
}

object Files {
    fun exists(path: Path): Boolean = java.nio.file.Files.exists(path)
    fun notExists(path: Path): Boolean = java.nio.file.Files.notExists(path)

    fun createFile(path: Path): Path = java.nio.file.Files.createFile(path)
    fun createDirectories(path: Path): Path = java.nio.file.Files.createDirectories(path)

    fun list(dir: Path): Sequence<Path> = java.nio.file.Files.list(dir).iterator().asSequence()
}