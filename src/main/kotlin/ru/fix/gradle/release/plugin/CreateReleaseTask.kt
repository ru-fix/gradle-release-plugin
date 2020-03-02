package ru.fix.gradle.release.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class CreateReleaseTask : DefaultTask() {

    /**
     * Search for released versions based on existing tag names and creates
     * new tag with incremented version
     */
    @TaskAction
    fun createRelease() {
        BranchGardener(project, UserInteractor(project, services)).createRelease()
    }
}