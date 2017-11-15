package ru.fix.platform.plugin.release

import org.eclipse.jgit.api.Git
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

open class CreateReleaseTask : DefaultTask() {

    @TaskAction
    fun createRelease() {

        var extension = project.extensions.findByType(ReleaseExtension::class.java)
        extension = checkNotNull(extension)

        val git = ReleaseUtils.createGit();


        if (extension.baseVersion == null) {
            throw GradleException("Please specify base version")
        }

        val targetBranch = "${extension.releaseBranchPrefix}${extension.baseVersion}"

        if (!ReleaseUtils.isBranchExists(git, targetBranch)) {
            throw GradleException("Please create release branch first - see :createReleaseBranch task")
        }

        git.checkout().setName(targetBranch)

        val existingVersions = getExistingVersions(git)

        val version = if (existingVersions.isEmpty()) {
            "${extension.baseVersion}.1"
        } else {
            "1.0" //TODO: select
        }


        val tempBranch = "final_$targetBranch";

        if (ReleaseUtils.isBranchExists(git, tempBranch)) {
            throw GradleException("Temporary branch $tempBranch already exists")
        }

        git.checkout().setName(tempBranch)
                .setCreateBranch(true).call()

        //TODO: modify and commit files


        val tree = project.fileTree("./")
                .include("gradle.properties")


//        tree = fileTree('src').include('**/*.java')


        git.tag()
                .setAnnotated(true)
                .setName(version)
                .call()


    }

    private fun checkValidBranch(branchPrefix: String, currentBranch: String) {
        if (!Regex("$branchPrefix-(\\d+)\\.(\\d+)").matches(currentBranch)) {
            throw GradleException("Invalid release branch")
        }
    }

    private fun getExistingVersions(git: Git): List<String> {
        return git.tagList()
                .call()
                .map { checkNotNull(it.name) }
                .filter { Regex("(\\d+)\\.(\\d+).(\\d+)").matches(it) }
    }

}