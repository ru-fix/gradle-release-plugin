package ru.fix.gradle.release.plugin

import org.eclipse.jgit.errors.UnsupportedCredentialItem
import org.eclipse.jgit.transport.CredentialItem
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.URIish
import org.gradle.api.Project
import ru.fix.gradle.release.plugin.PluginProperties.GIT_LOGIN
import ru.fix.gradle.release.plugin.PluginProperties.GIT_PASSWORD


class GitCredentialsProvider(
    private val project: Project,
    private val userInteractor: UserInteractor
) : CredentialsProvider() {

    private val login by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { resolveLogin() }
    private val password by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { resolvePassword() }

    override fun isInteractive(): Boolean {
        return true
    }

    fun resolveLogin(): String {
        project.logger.lifecycle("Looking for gradle/system property ${PluginProperties.GIT_LOGIN}")

        GIT_LOGIN.fromProjectOrSystem(project)?.let {
            return it
        }
        return userInteractor.promptQuestion("Please, enter your login: ")
    }

    fun resolvePassword(): CharArray {
        project.logger.lifecycle("Looking for gradle/system property ${PluginProperties.GIT_PASSWORD}")

        GIT_PASSWORD.fromProjectOrSystem(project)?.let {
            return it.toCharArray()
        }
        return userInteractor.promptPassword("Please, enter your password: ")
    }

    override fun get(uri: URIish, vararg credentialItems: CredentialItem): Boolean {
        for (credentialItem in credentialItems) {
            when (credentialItem) {
                is CredentialItem.Username -> {
                    credentialItem.value = login
                }
                is CredentialItem.Password -> {
                    credentialItem.value = password
                }
                is CredentialItem.InformationalMessage -> {
                    project.logger.lifecycle("Git: " + credentialItem.promptText)
                }
                else -> throw UnsupportedCredentialItem(
                    uri,
                    credentialItem.javaClass.name + ":" + credentialItem.promptText
                )
            }
        }
        return true
    }

    override fun supports(vararg credentailItems: CredentialItem): Boolean {
        for (credentialItem in credentailItems) {
            if (credentialItem is CredentialItem.Username)
                continue
            if (credentialItem is CredentialItem.Password)
                continue

            return false
        }
        return true
    }
}