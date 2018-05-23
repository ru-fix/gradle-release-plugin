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

        var extension = project.extensions.findByType(ReleaseExtension::class.java)
        extension = checkNotNull(extension)

        if (project.hasProperty("baseVersion")) {
            val baseVersion = project.property("baseVersion").toString()

            if (!VersionUtils.isValidBranchVersion(baseVersion)) {
                throw GradleException("Invalid base version: $baseVersion. Should be in x.y format")
            }


            val targetBranch = "${extension.releaseBranchPrefix}$baseVersion"
            if (GitUtils.getCurrentBranch() != targetBranch) {
                project.logger.lifecycle("Switching to release branch $targetBranch")

                val remote = project.hasProperty("remoteCheckout")
                        && project.property("remoteCheckout")
                        .toString().toBoolean()

                GitUtils.checkoutBranch(targetBranch, remote)
            }
        }

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

        with(GitUtils) {

            if (isBranchExists(tempBranch)) {
                throw GradleException("Temporary branch $tempBranch already exists. Please delete it first")
            }

            createBranch(tempBranch, true)
            fileList.forEach { VersionUtils.updateVersionInFile(it.absolutePath, version) }

            commitFilesInIndex("Updating version to $version")
            val tagRef = createTag(version, "Release $version")


            if (project.hasProperty("checkoutTag") &&
                    project.property("checkoutTag").toString().toBoolean()) {
                checkoutTag(version)
            } else {
                checkoutBranch(branch, false)
            }

            deleteBranch(tempBranch)


            if (isCredentialsSupplied()) {
                val gitLogin = project.property(GitUtils.GIT_LOGIN_PARAMETER).toString()
                val gitPassword = project.property(GitUtils.GIT_PASSWORD_PARAMETER).toString()
                logger.lifecycle("Pushing with login $gitLogin")
                pushTag(gitLogin, gitPassword, tagRef)

            } else {
                logger.lifecycle("Git credentials weren't supplied, try to push via ssh")

                try {
                    pushTagViaSsh(tagRef)
                } catch (exc: Exception) {
                    logger.debug("Skip ssh push because of: ${exc.message}", exc)
                    logger.lifecycle("Failed to push via ssh. You have to manually push changes to remote repository.")
                }
            }

            logger.lifecycle("Completed successfully")
        }
    }

    private fun checkValidBranch(branchPrefix: String, currentBranch: String) {
        logger.lifecycle("Checking branch $currentBranch for validity")
        if (!Regex("$branchPrefix(\\d+)\\.(\\d+)").matches(currentBranch)) {
            throw GradleException("Invalid release branch")
        }
    }

    private fun isCredentialsSupplied(): Boolean {
        return project.hasProperty(GitUtils.GIT_LOGIN_PARAMETER)
                && project.hasProperty(GitUtils.GIT_PASSWORD_PARAMETER);
    }

}