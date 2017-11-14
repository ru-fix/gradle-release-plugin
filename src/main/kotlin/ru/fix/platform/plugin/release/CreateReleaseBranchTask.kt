package ru.fix.platform.plugin.release

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

open class CreateReleaseBranchTask : DefaultTask() {

    @TaskAction
    fun createReleaseBranch() {

        val extension = project?.extensions?.findByType(ReleaseExtension::class.java)

        val builder = FileRepositoryBuilder()
        val repository = builder.readEnvironment()
                .findGitDir()
                .build()

        val git = Git(repository)
        val currentBranch = repository.branch

        if(extension == null){
            throw GradleException("Extension is null")
        }




    }



}