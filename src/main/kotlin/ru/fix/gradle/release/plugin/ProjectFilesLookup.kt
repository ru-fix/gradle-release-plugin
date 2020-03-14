package ru.fix.gradle.release.plugin

import org.gradle.api.Project
import java.nio.file.Path


class ProjectFilesLookup(
        private val project: Project,
        private val userInteractor: UserInteractor) {

    fun openGitRepository(): GitRepository {
        val repo = GitRepository.openExisting(
                project.projectDir,
                GitCredentialsProvider(project, userInteractor))
        userInteractor.info("Found git repository at: ${repo.directory}")
        return repo
    }

    fun findGradlePropertiesFile(): Path {
        val fileName = "gradle.properties"
        userInteractor.info("Looking for '$fileName' at '${project.projectDir.absolutePath}'")
        val file = project.projectDir.listFiles()
                .filter { it.isFile }
                .filter { it.name == fileName }
                .singleOrNull()
        if (file != null)
            return file.toPath()
        else {
            throw Exception("Failed to find '$fileName' at '${project.projectDir.absolutePath}'")
        }
    }
}