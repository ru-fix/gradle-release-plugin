package ru.fix.platform.plugin.release

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import java.io.File

open class CreateReleaseTask : DefaultTask() {

    @TaskAction
    fun createRelease() {

        var extension = project.extensions.findByType(ReleaseExtension::class.java)
        extension = checkNotNull(extension)


        val branch = GitUtils.getCurrentBranch();

        checkValidBranch(extension.releaseBranchPrefix, branch)
        val baseVersion = VersionUtils.extractVersionFromBranch(branch);

        val version = VersionUtils.supposeReleaseVersion(baseVersion)

        project.logger.lifecycle("Creating release for version $version")

        val files = File("./").walkTopDown()
                .filter { it.name == "gradle.properties" }

        val fileList = files.toList();

        if (fileList.isEmpty()) {
            throw GradleException("There are no gradle.properties in project. Terminating")
        }


        val tempBranch = "final_${extension.releaseBranchPrefix}$version"

        if (GitUtils.isBranchExists(tempBranch)) {
            throw GradleException("Temporary branch $tempBranch already exists. Please delete it first")
        }

        GitUtils.createBranch(tempBranch, true)

        fileList.forEach { VersionUtils.updateVersionInFile(it.absolutePath, version) }

        GitUtils.commitFilesInIndex("Updating version to $version")
        GitUtils.createTag(version, "Release $version")

        GitUtils.checkout(branch)
        GitUtils.deleteBranch(tempBranch)

        //TODO: push
    }

    private fun checkValidBranch(branchPrefix: String, currentBranch: String) {
        if (!Regex("$branchPrefix(\\d+)\\.(\\d+)").matches(currentBranch)) {
            throw GradleException("Invalid release branch")
        }
    }

}