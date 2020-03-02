package ru.fix.gradle.release.plugin

import org.gradle.api.Project
import org.gradle.api.logging.Logging


class GitExtensionConfiguration(
        private val project: Project,
        private val userInteractor: UserInteractor) {
    private val logger = Logging.getLogger(GitExtensionConfiguration::class.simpleName)

    fun openGitRepository(): GitClient {

        val git = GitClient(GitCredentialsProvider(project, userInteractor))

        if (git.find(project.projectDir)) {
            logger.lifecycle("Found git repository at: ${git.directory}")
            return git
        } else {
            "Failed to find git repository within: ${project.projectDir}".let {
                logger.error(it)
                throw Exception(it)
            }

        }
    }
}