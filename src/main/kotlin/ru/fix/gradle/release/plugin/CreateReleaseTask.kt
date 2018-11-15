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
        val git = GitExtensionConfiguration(project).buildGitClient()
        val versionManager = VersionManager(git)

        git.fetchTags()

        val extension = project.extensions.findByType(ReleaseExtension::class.java)
        checkNotNull(extension) { "Failed to find ReleaseExtension" }

        val branch = git.getCurrentBranch()

        checkValidBranch(extension.releaseBranchPrefix, branch)
        val baseVersion = versionManager.extractVersionFromBranch(branch)

        val version = versionManager.supposeReleaseVersion(baseVersion)

        project.logger.lifecycle("Creating release for version $version")

        val files = File("./").walkTopDown()
                .filter { it.name == "gradle.properties" }

        val fileList = files.toList();

        if (fileList.isEmpty()) {
            throw GradleException("There are no gradle.properties in project. Terminating")
        }

        val tempBranch = "temp_release_${extension.releaseBranchPrefix}$version"

        with(git) {

            if (isBranchExists(tempBranch)) {
                throw GradleException("Temporary branch $tempBranch already exists. Please delete it first")
            }

            createBranch(tempBranch, true)
            fileList.forEach { versionManager.updateVersionInFile(it.absolutePath, version) }

            commitFilesInIndex("Updating version to $version")
            val tagRef = createTag(version, "Release $version")


            if (project.hasProperty("checkoutTag") &&
                    project.property("checkoutTag").toString().toBoolean()) {
                checkoutTag(version)
            } else {
                checkoutLocalBranch(branch)
            }

            deleteBranch(tempBranch)

            pushTag(tagRef)
        }
    }

    private fun checkValidBranch(branchPrefix: String, currentBranch: String) {
        logger.lifecycle("Checking branch $currentBranch for validity")
        if (!Regex("$branchPrefix(\\d+)\\.(\\d+)").matches(currentBranch)) {
            throw GradleException("Invalid release branch")
        }
    }

}