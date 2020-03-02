package ru.fix.gradle.release.plugin

import org.gradle.api.Project
import org.gradle.api.internal.tasks.userinput.UserInputHandler
import org.gradle.internal.service.ServiceRegistry

class UserInteractor(
        private val project: Project,
        private val serviceRegistry: ServiceRegistry) {

    fun promptQuestion(prompt: String, default: String? = null): String {

        try {
            val userInputHandler = serviceRegistry.get(UserInputHandler::class.java)
            if (userInputHandler != null) {
                val inputHandlerInput = userInputHandler.askQuestion(prompt, default ?: "")
                if (inputHandlerInput != null) {
                    return inputHandlerInput
                }
            }
        } catch (exc: Exception) {
            project.logger.debug("Failed to user UserInputHandler", exc)
        }

        val console = System.console()
        if(console != null){
            return console.readLine(prompt) ?: default ?: throw IllegalArgumentException("Failed to read user input")
        }

        project.logger.lifecycle(prompt)
        return readLine() ?: default ?: throw IllegalArgumentException("Failed to read user input")
    }



    fun promptPassword(prompt: String): CharArray {
        try {
            val userInputHandler = serviceRegistry.get(UserInputHandler::class.java)
            if (userInputHandler != null) {
                val inputHandlerInput = userInputHandler.askQuestion(prompt, "")
                if (inputHandlerInput != null) {
                    return inputHandlerInput.toCharArray()
                }
            }
        } catch (exc: Exception) {
            project.logger.debug("Failed to user UserInputHandler", exc)
        }

        val console = System.console()
        if (console != null) {
            return console.readPassword(prompt) ?: throw IllegalArgumentException("Failed to read user input")
        }

        project.logger.lifecycle(prompt)
        return readLine()?.toCharArray() ?: throw IllegalArgumentException("Failed to read user input")
    }
}