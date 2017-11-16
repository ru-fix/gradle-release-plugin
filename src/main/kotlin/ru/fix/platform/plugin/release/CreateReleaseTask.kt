package ru.fix.platform.plugin.release

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

open class CreateReleaseTask : DefaultTask() {

    @TaskAction
    fun createRelease() {

        var extension = project.extensions.findByType(ReleaseExtension::class.java)
        extension = checkNotNull(extension)


        if (extension.baseVersion == null) {
            throw GradleException("Please specify base version")
        }

        val releaseBranch = "${extension.releaseBranchPrefix}${extension.baseVersion}"

        if (!GitUtils.isBranchExists(releaseBranch)) {
            throw GradleException("Please create release branch first - see :createReleaseBranch task")
        }


        if (GitUtils.getCurrentBranch() != releaseBranch) {
            GitUtils.checkout(releaseBranch)
        }

        val version = VersionUtils.supposePatchVersion(extension.baseVersion!!)

        val tempBranch = "final_${extension.releaseBranchPrefix}$version"

        if (GitUtils.isBranchExists(tempBranch)) {
            throw GradleException("Temporary branch $tempBranch already exists. Please delete it first")
        }


        GitUtils.createBranch(tempBranch, true)

        val files = project.files("gradle.properties")
                .from("./")

        files.forEach{
            VersionUtils.updateVersionInFile(it.absolutePath, version)
        }


        GitUtils.createTag(version, "Release $version")

        GitUtils.deleteBranch(tempBranch)

        //TODO: push

        GitUtils.checkout(extension.mainBranch)


    }

}