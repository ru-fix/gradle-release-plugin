package ru.fix.gradle.release.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class CreateReleaseBranchTask : DefaultTask() {

    @TaskAction
    fun createReleaseBranch() {
        BranchGardener(project, UserInteractor(project)).createReleaseBranch()
    }
}