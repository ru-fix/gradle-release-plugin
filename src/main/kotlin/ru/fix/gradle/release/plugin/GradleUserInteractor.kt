package ru.fix.gradle.release.plugin

import org.gradle.api.Project
import org.gradle.api.logging.LogLevel

class GradleUserInteractor(private val project: Project) : UserInteractor {

    private val logger = project.logger

    override fun promptQuestion(prompt: String, default: String?): String {
        val console = System.console()
        if (console != null) {
            return console.readLine(prompt) ?: default ?: throw IllegalArgumentException("Failed to read user input")
        }

        while (true) {
            logger.lifecycle(prompt)
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


    override fun promptPassword(prompt: String): CharArray {
        val console = System.console()
        if (console != null) {
            return console.readPassword(prompt) ?: throw IllegalArgumentException("Failed to read user input")
        }

        while (true) {
            logger.lifecycle(prompt)
            val line = readLine()
            if (!line.isNullOrBlank()) {
                return line.toCharArray()
            }
        }
    }

    override fun info(msg: String) {
        logger.log(LogLevel.LIFECYCLE, msg)
    }

    override fun error(msg: String){
        logger.log(LogLevel.ERROR, msg)
    }

}