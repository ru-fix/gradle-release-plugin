package ru.fix.gradle.release.plugin

import org.gradle.api.Project

object PluginProperties {
    class Property<T>(val name: String) {
        fun fromProject(project: Project): T? {
            return if (project.hasProperty(name)) {
                project.property(name)?.takeIf { it.toString().isNotEmpty() } as? T
            } else {
                null
            }
}

        fun fromSystem(): T? =
                System.getProperty(name)?.takeIf { it.toString().isNotEmpty() } as? T

        fun fromProjectOrSystem(project: Project): T? =
                fromProject(project) ?: fromSystem()

    }

    val GIT_LOGIN = Property<String>("ru.fix.gradle.release.login")
    val GIT_PASSWORD = Property<String>("ru.fix.gradle.release.password")
    val CHECKOUT_TAG = Property<Boolean>("ru.fix.gradle.release.checkoutTag")
    val RELEASE_MAJOR_MINOR_VERSION = Property<String>("ru.fix.gradle.release.releaseMajorMinorVersion")
    val CREATE_DEFAULT_RELEASE_BRANCH = Property<Boolean>("ru.fix.gradle.release.createDefaultReleaseBranch")
    val DRY_RUN = Property<Boolean>("ru.fix.gradle.release.dryRun")
}

