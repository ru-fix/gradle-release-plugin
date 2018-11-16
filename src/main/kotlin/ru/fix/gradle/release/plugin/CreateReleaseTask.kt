package ru.fix.gradle.release.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import java.io.File

open class CreateReleaseTask : DefaultTask() {

    /**
     * Search for released versions based on existing tag names and creates
     * new tag with incremented version
     */
    @TaskAction
    fun createRelease() {
        BranchGardener(project).createRelease()
    }
}