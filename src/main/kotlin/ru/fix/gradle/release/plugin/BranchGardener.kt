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
        // but user can specify explicitly which branch to use to create release
        if (project.hasProperty(ProjectProperties.RELEASE_BRANCH)) {
            val releaseBranch = project.property(ProjectProperties.RELEASE_BRANCH).toString()
            switchToUserDefinedReleaseBranch(releaseBranch, git)
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

    private fun switchToUserDefinedReleaseBranch(releaseBranch: String, git: GitRepository) {
        userInteractor.info("Using user defined branch: $releaseBranch")

        if (git.getCurrentBranch() != releaseBranch) {
            userInteractor.info("Switching to release branch $releaseBranch")
            if (git.isLocalBranchExists(releaseBranch)) {
                git.checkoutLocalBranch(releaseBranch)
            } else {
                git.checkoutRemoteBranch(releaseBranch)
            }
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

        val userVersion = userInteractor.promptQuestion(
                "Please specify release version in x.y format (Default: $supposedVersion)",
                supposedVersion)

        if (versionManager.branchVersionExists(userVersion)) {
            userInteractor.info("Version $userVersion already exists")
            return
        }

        if (!versionManager.isValidBranchVersion(userVersion)) {
            userInteractor.info("Please specify valid version in x.y format, current is $userVersion")
            return
        }

        val branch = "${extension.releaseBranchPrefix}$userVersion"

        if (git.isLocalBranchExists(branch)) {
            userInteractor.info("Branch with name $branch already exists")
            return
        }

        git.createBranch(branch, true)

        userInteractor.info("Branch $branch was successfully created based on $currentBranch")
    }
}