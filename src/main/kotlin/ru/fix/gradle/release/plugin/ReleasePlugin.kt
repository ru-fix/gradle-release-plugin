package ru.fix.gradle.release.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class ReleasePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.extensions.create("ru.fix.gradle.release", ReleaseExtension::class.java)
        project.tasks.create("createRelease", CreateReleaseTask::class.java)
        project.tasks.create("createReleaseBranch", CreateReleaseBranchTask::class.java)
    }


}