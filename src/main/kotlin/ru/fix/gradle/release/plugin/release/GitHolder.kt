package ru.fix.gradle.release.plugin.release

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

object GitHolder {

    val git = Git(FileRepositoryBuilder()
            .readEnvironment()
            .findGitDir()
            .build())

}