package ru.fix.gradle.release.plugin

import org.eclipse.jgit.api.Git
import org.gradle.api.Project
import org.gradle.api.logging.Logging


class GitExtensionConfiguration(private val project: Project) {
    private val logger = Logging.getLogger(GitExtensionConfiguration::class.simpleName)

    fun openGitRepository(): GitClient {

        val git: GitClient

        if (isCredentialsSupplied()) {
            val login = project.property(ProjectProperties.GIT_LOGIN).toString()
            val password = project.property(ProjectProperties.GIT_PASSWORD).toString()

            project.logger.lifecycle("Git credentials are supplied for $login.")
            git = GitClient(GitCredentials(login, password))
        } else {
            project.logger.lifecycle("Git credentials are not supplied.")
            git = GitClient()
        }

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

    private fun isCredentialsSupplied(): Boolean {
        return project.hasProperty(ProjectProperties.GIT_LOGIN) && project.hasProperty(ProjectProperties.GIT_PASSWORD)
    }
}