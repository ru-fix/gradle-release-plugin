package ru.fix.gradle.release.plugin

import org.gradle.api.Project
import java.io.File


class ProjectFilesLookup(
        private val project: Project,
        private val userInteractor: UserInteractor) {

    fun openGitRepository(): GitClient {
        val git = GitClient(GitCredentialsProvider(project, userInteractor))

        if (git.find(project.projectDir)) {
            userInteractor.info("Found git repository at: ${git.directory}")
            return git
        } else {
            throw Exception("Failed to find git repository within: ${project.projectDir}")

        }
    }

    fun findGradlePropertiesFile(): File {
        val fileName = "gradle.properties"
        userInteractor.info("Looking for '$fileName' at '${project.projectDir.absolutePath}'")
        val file = project.projectDir.listFiles()
                .filter { it.isFile }
                .filter { it.name == fileName }
                .singleOrNull()
        if (file != null)
            return file
        else {
            throw Exception("Failed to find '$fileName' at '${project.projectDir.absolutePath}'")
        }
    }
}