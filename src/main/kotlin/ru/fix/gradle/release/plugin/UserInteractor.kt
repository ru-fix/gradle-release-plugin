package ru.fix.gradle.release.plugin

import org.gradle.api.Project

class UserInteractor(
        private val project: Project) {

    fun promptQuestion(prompt: String, default: String? = null): String {
        val console = System.console()
        if (console != null) {
            return console.readLine(prompt) ?: default ?: throw IllegalArgumentException("Failed to read user input")
        }

        while (true) {
            project.logger.lifecycle(prompt)
            val line = readLine()
            if (line.isNullOrBlank()) {
                if (default != null) {
                    return default
                }
            } else {
                return line
            }
        }
    }


    fun promptPassword(prompt: String): CharArray {
        val console = System.console()
        if (console != null) {
            return console.readPassword(prompt) ?: throw IllegalArgumentException("Failed to read user input")
        }

        while (true) {
            project.logger.lifecycle(prompt)
            val line = readLine()
            if (!line.isNullOrBlank()) {
                return line.toCharArray()
            }
        }
    }
}