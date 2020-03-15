package ru.fix.gradle.release.plugin

import org.gradle.api.GradleException
import org.gradle.api.Project


class BranchGardener(
        private val project: Project,
        private val userInteractor: UserInteractor,
        private val projectFileSystemLookup: ProjectFilesLookup) {

    /**
     * Search for released versions based on existing tag names and creates
     * new tag with incremented version
     */
    fun createRelease() {
        val git = projectFileSystemLookup.openGitRepository()

        val versionManager = VersionManager(git, userInteractor)

        if (git.isUncommittedChangesExist()) {
            userInteractor.error("" +
                    "Could not create new release due to uncommitted changes. " +
                    "Please commit your current work before creating new release.")
            return
        }

        git.fetchTags()

        val extension = project.extensions.findByType(ReleaseExtension::class.java)
        checkNotNull(extension) { "Failed to find ReleaseExtension" }


        // by default current branch is used as release branch
        // but user can specify explicitly which branch
        if (project.hasProperty(ProjectProperties.RELEASE_BRANCH_VERSION)) {
            val releaseBranchVersion = project.property(ProjectProperties.RELEASE_BRANCH_VERSION).toString()
            userInteractor.info("Using user defined branch version: $releaseBranchVersion")

            if (!versionManager.isValidBranchVersion(releaseBranchVersion)) {
                throw GradleException("Invalid release branch version: $releaseBranchVersion. Should be in x.y format")
            }

            val targetBranch = "${extension.releaseBranchPrefix}$releaseBranchVersion"
            if (git.getCurrentBranch() != targetBranch) {
                userInteractor.info("Switching to release branch $targetBranch")

                if (git.isLocalBranchExists(targetBranch)) {
                    git.checkoutLocalBranch(targetBranch)
                } else {
                    git.checkoutRemoteBranch(targetBranch)
                }
            }
        }

        val branch = git.getCurrentBranch()
        if (!checkAndInformUserIfCurrentBanchIsInvalid(extension.releaseBranchPrefix, branch)) {
            return
        }

        val baseVersion = versionManager.extractVersionFromBranch(branch)

        val version = versionManager.supposeReleaseVersion(baseVersion)

        userInteractor.info("Creating release for version $version")

        val gradlePropertiesFile = projectFileSystemLookup.findGradlePropertiesFile()

        val tempBranch = "temp_gradle_release_plugin/${extension.releaseBranchPrefix}$version"

        with(git) {

            if (isLocalBranchExists(tempBranch)) {
                throw GradleException("Temporary branch $tempBranch already exists. Please delete it first")
            }

            createBranch(tempBranch, true)
            versionManager.updateVersionInFile(gradlePropertiesFile.toAbsolutePath(), version)

            commitFilesInIndex(extension.commitMessage(version))
            val tagRef = createTag(extension.tagName(version), extension.commitMessage(version))

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

    private fun checkAndInformUserIfCurrentBanchIsInvalid(branchPrefix: String, currentBranch: String): Boolean {
        val pattern = "$branchPrefix(\\d+)\\.(\\d+)"

        userInteractor.info("Checking that branch '$currentBranch' matches release branch naming pattern '$pattern'")
        if (!Regex(pattern).matches(currentBranch)) {
            userInteractor.error("Current branch $currentBranch does not match pattern '$pattern'")
            return false
        }
        return true
    }

    fun createReleaseBranch() {
        val git = projectFileSystemLookup.openGitRepository()

        val versionManager = VersionManager(git, userInteractor)

        if (git.isUncommittedChangesExist()) {
            userInteractor.error("" +
                    "Could not create new release branch due to uncommitted changes. " +
                    "Please commit your current work before creating new release branch.")
            return
        }

        val extension = project.extensions.findByType(ReleaseExtension::class.java)
        checkNotNull(extension) { "Failed to find ReleaseExtension" }

        val currentBranch = git.getCurrentBranch()
        userInteractor.info("Creating new release branch based on: $currentBranch")

        val supposedVersion = versionManager.supposeBranchVersion()


        var input = userInteractor.promptQuestion(
                "Please specify release version in x.y format (Default: $supposedVersion)",
                supposedVersion)

        if (versionManager.branchVersionExists(input)) {
            userInteractor.info("Version $input already exists")
            return
        }

        if (!versionManager.isValidBranchVersion(input)) {
            userInteractor.info("Please specify valid version")
            return
        }

        val branch = "${extension.releaseBranchPrefix}$input"

        if (git.isLocalBranchExists(branch)) {
            userInteractor.info("Branch with name $branch already exists")
            return
        }

        git.createBranch(branch, true)

        userInteractor.info("Branch $branch was successfully created based on $currentBranch")
    }
}