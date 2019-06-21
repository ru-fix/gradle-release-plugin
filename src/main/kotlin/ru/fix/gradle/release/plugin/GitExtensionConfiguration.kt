package ru.fix.gradle.release.plugin

import org.gradle.api.Project


class GitExtensionConfiguration(private val project: Project) {
    fun buildGitClient(): GitClient {

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

        git.find(project.projectDir)

        return git
    }

    private fun isCredentialsSupplied(): Boolean {
        return project.hasProperty(ProjectProperties.GIT_LOGIN) && project.hasProperty(ProjectProperties.GIT_PASSWORD)
    }
}