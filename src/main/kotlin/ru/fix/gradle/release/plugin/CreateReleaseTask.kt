package ru.fix.gradle.release.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.internal.tasks.userinput.UserInputHandler
import org.gradle.api.tasks.TaskAction


open class CreateReleaseTask : DefaultTask() {

    @TaskAction
    fun createRelease() {

        val userInputHandler = services.get(UserInputHandler::class.java)
        val userInteractor = GradleUserInteractor(project, userInputHandler)

        try {
            BranchGardener(
                    project = project,
                    userInteractor = userInteractor,
                    projectFileSystemLookup = ProjectFilesLookup(project, userInteractor)).createRelease()
        }catch (exc: Exception){
            userInteractor.error(exc.message ?: "")
            throw exc
        }
    }
}