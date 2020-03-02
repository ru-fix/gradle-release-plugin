package ru.fix.gradle.release.plugin

import org.eclipse.jgit.errors.UnsupportedCredentialItem
import org.eclipse.jgit.transport.CredentialItem
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.URIish
import org.gradle.api.Project


class GitCredentialsProvider(
        private val project: Project) : CredentialsProvider() {
    override fun isInteractive(): Boolean {
        return true
    }

    fun resolveLogin(): String {
        if (project.hasProperty(ProjectProperties.GIT_LOGIN)) {
            val propertyLogin = project.property(ProjectProperties.GIT_LOGIN).toString()
            if (propertyLogin.isNotEmpty()) {
                return propertyLogin
            }
        }
        val systemPropertyLogin = System.getProperty(ProjectProperties.GIT_LOGIN)
        if (systemPropertyLogin != null && systemPropertyLogin.isNotEmpty()) {
            return systemPropertyLogin
        }

        val loginPrompt = "> Please, enter your login: "

        val console = System.console()
        if(console != null){
            return console.readLine(loginPrompt) ?: throw IllegalArgumentException("Failed to read user input")
        }

        print(loginPrompt)
        return readLine() ?: throw IllegalArgumentException("Failed to read user input")
    }

    fun resolvePassword(): CharArray {
        if (project.hasProperty(ProjectProperties.GIT_PASSWORD)) {
            val propertyPassword = project.property(ProjectProperties.GIT_PASSWORD).toString()
            if (propertyPassword.isNotEmpty()) {
                return propertyPassword.toCharArray()
            }
        }
        val systemPropertyPassword = System.getProperty(ProjectProperties.GIT_PASSWORD)
        if (systemPropertyPassword != null && systemPropertyPassword.isNotEmpty()) {
            return systemPropertyPassword.toCharArray()
        }

        val passwordPrompt = "> Please, enter your password: "
        val console = System.console()
        if(console != null) {
            return console.readPassword(passwordPrompt) ?: throw IllegalArgumentException("Failed to read user input")
        }

        print(passwordPrompt)
        return readLine()?.toCharArray() ?: throw IllegalArgumentException("Failed to read user input")
    }

    override fun get(uri: URIish, vararg credentialItems: CredentialItem): Boolean {
        for (credentialItem in credentialItems) {
            if (credentialItem is CredentialItem.Username) {
                credentialItem.value = resolveLogin()
                continue
            }
            if (credentialItem is CredentialItem.Password) {
                credentialItem.value = resolvePassword()
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