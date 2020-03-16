package ru.fix.gradle.release.plugin

import org.eclipse.jgit.errors.UnsupportedCredentialItem
import org.eclipse.jgit.transport.CredentialItem
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.URIish
import org.gradle.api.Project


class GitCredentialsProvider(
        private val project: Project,
        private val userInteractor: UserInteractor) : CredentialsProvider() {

    private val login by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { resolveLogin() }
    private val password by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { resolvePassword() }

    override fun isInteractive(): Boolean {
        return true
    }

    fun resolveLogin(): String {
        project.logger.lifecycle("Looking for gradle/system property ${PluginProperties.GIT_LOGIN}")
        if (project.hasProperty(PluginProperties.GIT_LOGIN)) {
            val propertyLogin = project.property(PluginProperties.GIT_LOGIN).toString()
            if (propertyLogin.isNotEmpty()) {
                return propertyLogin
            }
        }
        val systemPropertyLogin = System.getProperty(PluginProperties.GIT_LOGIN)
        if (systemPropertyLogin != null && systemPropertyLogin.isNotEmpty()) {
            return systemPropertyLogin
        }
        return userInteractor.promptQuestion("Please, enter your login: ")
    }

    fun resolvePassword(): CharArray {
        project.logger.lifecycle("Looking for gradle/system property ${PluginProperties.GIT_PASSWORD}")
        if (project.hasProperty(PluginProperties.GIT_PASSWORD)) {
            val propertyPassword = project.property(PluginProperties.GIT_PASSWORD).toString()
            if (propertyPassword.isNotEmpty()) {
                return propertyPassword.toCharArray()
            }
        }
        val systemPropertyPassword = System.getProperty(PluginProperties.GIT_PASSWORD)
        if (systemPropertyPassword != null && systemPropertyPassword.isNotEmpty()) {
            return systemPropertyPassword.toCharArray()
        }


        return userInteractor.promptPassword("Please, enter your password: ")
    }

    override fun get(uri: URIish, vararg credentialItems: CredentialItem): Boolean {
        for (credentialItem in credentialItems) {
            if (credentialItem is CredentialItem.Username) {
                credentialItem.value = login
                continue
            }
            if (credentialItem is CredentialItem.Password) {
                credentialItem.value = password
                continue
            }
            throw UnsupportedCredentialItem(uri, credentialItem.javaClass.name + ":" + credentialItem.promptText)
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