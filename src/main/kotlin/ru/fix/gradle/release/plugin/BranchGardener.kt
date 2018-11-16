package ru.fix.gradle.release.plugin

import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.File


class BranchGardener(private val project: Project) {

    fun createRelease() {
        val git = GitExtensionConfiguration(project).buildGitClient()
        val versionManager = VersionManager(git)

        if (git.isUncommittedChangesExist()) {
            project.logger.lifecycle("" +
                    "Could not create new release due to uncommitted changes. " +
                    "Please commit your current work before creating new release.")
            return
        }

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


            if (project.hasProperty(ProjectProperties.CHECKOUT_TAG) &&
                    project.property(ProjectProperties.CHECKOUT_TAG).toString().toBoolean()) {
                checkoutTag(version)
            } else {
                checkoutLocalBranch(branch)
            }

            deleteBranch(tempBranch)

            pushTag(tagRef)
        }
    }

    private fun checkValidBranch(branchPrefix: String, currentBranch: String) {
        project.logger.lifecycle("Checking branch $currentBranch matches release branch naming pattern")
        if (!Regex("$branchPrefix(\\d+)\\.(\\d+)").matches(currentBranch)) {
            throw GradleException("Invalid release branch")
        }
    }

    fun createReleaseBranch() {
        val git = GitExtensionConfiguration(project).buildGitClient()
        val versionManager = VersionManager(git)

        if (git.isUncommittedChangesExist()) {
            project.logger.lifecycle("Will not create release due to uncommitted changes")
            return
        }

        val extension = project.extensions.findByType(ReleaseExtension::class.java)
        checkNotNull(extension) { "Failed to find ReleaseExtension" }


        if (git.getCurrentBranch() != extension.mainBranch) {
            throw GradleException("Release branch can be built only from ${extension.mainBranch} branch")
        }

        val supposedVersion = versionManager.supposeBranchVersion()
        project.logger.lifecycle("Please specify release version (Should be in x.y format) [$supposedVersion]")


        while (true) {

            var input = readLine()

            if (input == null || input.isBlank()) {
                input = supposedVersion
            }

            if (versionManager.branchVersionExists(input)) {
                project.logger.lifecycle("Version $input already exists")
                continue
            }

            if (!versionManager.isValidBranchVersion(input)) {
                project.logger.lifecycle("Please specify valid version")
                continue
            }

            val branch = "${extension.releaseBranchPrefix}$input"

            if (git.isBranchExists(branch)) {
                project.logger.lifecycle("Branch with name $branch already exists")
                continue
            }

            git.createBranch(branch, true)

            project.logger.lifecycle("Branch $branch was successfully created")
            break

        }
    }
}