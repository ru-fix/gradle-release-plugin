package ru.fix.gradle.release.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.internal.tasks.userinput.UserInputHandler
import org.gradle.api.tasks.TaskAction

open class CreateReleaseBranchTask : DefaultTask() {

    @TaskAction
    fun createReleaseBranch() {
        val userInputHandler = services.get(UserInputHandler::class.java)
        val userInteractor = GradleUserInteractor(project, userInputHandler)
        try {
            BranchGardener(
                    project,
                    userInteractor,
                    ProjectFilesLookup(project, userInteractor)).createReleaseBranch()
        } catch (exc: Exception) {
            userInteractor.error(exc.message ?: "")
            throw exc
        }
    }
}