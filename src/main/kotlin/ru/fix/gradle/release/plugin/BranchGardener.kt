package ru.fix.gradle.release.plugin

import org.gradle.api.GradleException
import org.gradle.api.Project
import ru.fix.gradle.release.plugin.PluginProperties.CREATE_DEFAULT_RELEASE_BRANCH


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
            if (PluginProperties.DRY_RUN.fromProjectOrSystem(project) == true) {
                userInteractor.info("Found uncommitted changes. Ignoring ")
            } else {
            userInteractor.error("" +
                    "Could not create new release due to uncommitted changes. " +
                    "Please commit your current work before creating new release.")
            return
        }
        }

        git.fetchTags()

        val extension = project.extensions.findByType(ReleaseExtension::class.java)
        checkNotNull(extension) { "Failed to find ReleaseExtension" }

        // by default current branch is used as release branch
        // but user can specify explicitly which major and minor version to use to create release
        val userDefinedMajorMinorVersion: String? =
                PluginProperties.RELEASE_MAJOR_MINOR_VERSION.fromProjectOrSystem(project)?.let { majorMinorVersion ->
                    versionManager.assertValidMajorMinorVersion(majorMinorVersion)
                    majorMinorVersion
                }

        val branch: String
        val fullVersion: String

        when (extension.nextReleaseVersionDeterminationSchema) {
            ReleaseDetection.MAJOR_MINOR_FROM_BRANCH_NAME_PATCH_FROM_TAG -> {
                if (userDefinedMajorMinorVersion != null) {
                    switchToUserDefinedReleaseBranch(extension.releaseBranchPrefix + userDefinedMajorMinorVersion, git)
                }
                branch = git.getCurrentBranch()

                assertBranchIsAReleaseBranch(extension.releaseBranchPrefix, branch)

                val majorMinorVersionFromBranch = versionManager.extractVersionFromBranch(branch)
                fullVersion = versionManager.supposeReleaseVersion(majorMinorVersionFromBranch)
            }
            ReleaseDetection.MAJOR_MINOR_PATCH_FROM_TAG -> {
                branch = git.getCurrentBranch()
                fullVersion = if (userDefinedMajorMinorVersion != null)
                    versionManager.supposeReleaseVersion(userDefinedMajorMinorVersion)
                else
                    versionManager.supposeReleaseVersion()
            }
        }

        userInteractor.info("Creating release for version $fullVersion")

        val gradlePropertiesFile = projectFileSystemLookup.findGradlePropertiesFile()

        val tempBranch = "temp_gradle_release_plugin/$fullVersion"

        if (git.isLocalBranchExists(tempBranch)) {
            throw GradleException("Temporary branch $tempBranch already exists. Please delete it first")
        }

        git.createBranch(tempBranch, true)
        versionManager.updateVersionInFile(gradlePropertiesFile.toAbsolutePath(), fullVersion)

        git.commitFilesInIndex(extension.commitMessage(fullVersion))
        val tagRef = git.createTag(extension.tagName(fullVersion), extension.commitMessage(fullVersion))

        if (PluginProperties.CHECKOUT_TAG.fromProjectOrSystem(project) == true) {
            git.checkoutTag(fullVersion)
        } else {
            git.checkoutLocalBranch(branch)
        }

        git.deleteBranch(tempBranch)

        git.pushTag(tagRef)
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

    private fun assertBranchIsAReleaseBranch(branchPrefix: String, currentBranch: String) {
        val pattern = "$branchPrefix(\\d+)\\.(\\d+)"

        userInteractor.info("Checking that branch '$currentBranch' matches release branch naming pattern '$pattern'")
        if (!Regex(pattern).matches(currentBranch)) {
            throw BranchDoesNotMatchReleaseBranchNamingConvention(
                    "Current branch $currentBranch does not match pattern '$pattern'")

        }
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

        val userVersion = if (CREATE_DEFAULT_RELEASE_BRANCH.fromProjectOrSystem(project) == true) {
            supposedVersion
        } else {
            userInteractor.promptQuestion(
                    "Please specify release version in x.y format (Default: $supposedVersion)",
                    supposedVersion)
        }

        if (versionManager.branchVersionExists(userVersion)) {
            userInteractor.info("Version $userVersion already exists")
            return
        }

        versionManager.assertValidMajorMinorVersion(userVersion)

        val branch = "${extension.releaseBranchPrefix}$userVersion"

        if (git.isLocalBranchExists(branch)) {
            userInteractor.info("Branch with name $branch already exists")
            return
        }

        git.createBranch(branch, true)

        userInteractor.info("Branch $branch was successfully created based on $currentBranch")
    }
}

class BranchDoesNotMatchReleaseBranchNamingConvention(message: String) : Exception(message)