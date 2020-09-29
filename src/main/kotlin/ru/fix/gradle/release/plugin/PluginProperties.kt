package ru.fix.gradle.release.plugin

import org.gradle.api.Project

object PluginProperties {
    open class StringProperty(val name: String) {
        fun fromProject(project: Project): String? {
            return if (project.hasProperty(name)) {
                project.property(name)?.toString()?.takeIf { it.isNotEmpty() }
            } else {
                null
            }
        }

        fun fromSystem(): String? =
                System.getProperty(name)?.takeIf { it.isNotEmpty() }

        fun fromProjectOrSystem(project: Project): String? =
                fromProject(project) ?: fromSystem()
    }

    class BooleanProperty(val name: String) {
        private val property = StringProperty(name)
        fun fromProjectOrSystem(project: Project): Boolean? =
                property.fromProjectOrSystem(project)?.toBoolean()
    }

    val GIT_LOGIN = StringProperty("ru.fix.gradle.release.login")
    val GIT_PASSWORD = StringProperty("ru.fix.gradle.release.password")
    val CHECKOUT_TAG = BooleanProperty("ru.fix.gradle.release.checkoutTag")
    val RELEASE_MAJOR_MINOR_VERSION = StringProperty("ru.fix.gradle.release.releaseMajorMinorVersion")
    val CREATE_DEFAULT_RELEASE_BRANCH = BooleanProperty("ru.fix.gradle.release.createDefaultReleaseBranch")
    val DRY_RUN = BooleanProperty("ru.fix.gradle.release.dryRun")
}

