package com.ktome.game.validation

import java.nio.file.Path

object ValidationPaths {
    private const val ROOT_DIR_NAME: String = ".ktome"
    private const val VALIDATION_DIR_NAME: String = "validation"
    private const val SAVE_DIR_NAME: String = "save"
    private const val PROFILE_DIR_NAME: String = "profile"

    fun rootDir(homeDir: Path = defaultHomeDir()): Path =
        homeDir.resolve(ROOT_DIR_NAME).resolve(VALIDATION_DIR_NAME)

    fun saveDir(homeDir: Path = defaultHomeDir()): Path =
        rootDir(homeDir).resolve(SAVE_DIR_NAME)

    fun profileDir(homeDir: Path = defaultHomeDir()): Path =
        rootDir(homeDir).resolve(PROFILE_DIR_NAME)

    private fun defaultHomeDir(): Path = Path.of(System.getProperty("user.home"))
}
