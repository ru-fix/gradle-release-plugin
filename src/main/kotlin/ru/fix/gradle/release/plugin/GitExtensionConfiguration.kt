package ru.fix.gradle.release.plugin

import org.gradle.api.Project


const val GIT_LOGIN_PARAMETER = "git.login"
const val GIT_PASSWORD_PARAMETER = "git.password"


class GitExtensionConfiguration(private val project: Project) {
    fun buildGitClient(): GitClient {
        val login = project.property(GIT_LOGIN_PARAMETER).toString()
        val password = project.property(GIT_PASSWORD_PARAMETER).toString()

        return if (isCredentialsSupplied()) {
            project.logger.lifecycle("Git credentials are supplied for $login.")
            GitClient(GitCredentials(login, password))
        } else {
            project.logger.lifecycle("Git credentials are not supplied.")
            GitClient()
        }
    }

    private fun isCredentialsSupplied(): Boolean {
        return project.hasProperty(GIT_LOGIN_PARAMETER) && project.hasProperty(GIT_PASSWORD_PARAMETER)
    }
}