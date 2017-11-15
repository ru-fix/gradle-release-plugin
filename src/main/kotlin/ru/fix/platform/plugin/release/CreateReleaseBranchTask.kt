package ru.fix.platform.plugin.release

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import ru.fix.platform.plugin.release.ReleaseUtils.Companion.isValidVersion

open class CreateReleaseBranchTask : DefaultTask() {

    @TaskAction
    fun createReleaseBranch() {

        var extension = project.extensions.findByType(ReleaseExtension::class.java)
        extension = checkNotNull(extension)


        val git = ReleaseUtils.createGit();
        val currentBranch = git.repository.branch

        if (currentBranch != extension.mainBranch) {
            throw GradleException("Release branch can be built only from ${extension.mainBranch} branch")
        }

        print("Please specify release version (Should be in x.y format)")

        while (true) {
            val input = readLine()
            if (input == null || !isValidVersion(input)) {
                println("Please specify valid version")
                continue
            }

            val branch = "${extension.releaseBranchPrefix}$input"

            if (ReleaseUtils.isBranchExists(git, branch)) {
                println("Branch $branch already exists")
                continue;
            }

            ReleaseUtils.createBranch(git, branch)

            println("Branch $branch successfully created")
            break

        }
    }





}