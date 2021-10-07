package ru.fix.gradle.release.plugin

import org.gradle.api.Project
import org.gradle.api.internal.tasks.userinput.UserInputHandler
import org.gradle.api.logging.LogLevel


class GradleUserInteractor(project: Project, private val gradleUserInputHandler: UserInputHandler) : UserInteractor {

    private val logger = project.logger

    override fun promptQuestion(prompt: String, default: String?): String {
        return gradleUserInputHandler.askQuestion(prompt, default)
    }

    override fun promptPassword(prompt: String): CharArray {
        val input = gradleUserInputHandler.askQuestion(prompt, "")
        return input.toCharArray()
    }

    override fun info(msg: String) {
        logger.log(LogLevel.LIFECYCLE, msg)
    }

    override fun error(msg: String) {
        logger.log(LogLevel.ERROR, msg)
    }
}