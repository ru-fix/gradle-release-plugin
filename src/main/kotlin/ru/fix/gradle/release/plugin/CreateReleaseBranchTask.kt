package ru.fix.gradle.release.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class CreateReleaseBranchTask : DefaultTask() {

    @TaskAction
    fun createReleaseBranch() {
        val userInteractor = GradleUserInteractor(project)
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