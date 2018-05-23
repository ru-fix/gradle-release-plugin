package ru.fix.gradle.release.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

open class CreateReleaseBranchTask : DefaultTask() {

    @TaskAction
    fun createReleaseBranch() {

        var extension = project.extensions.findByType(ReleaseExtension::class.java)
        extension = checkNotNull(extension)


        if (GitUtils.getCurrentBranch() != extension.mainBranch) {
            throw GradleException("Release branch can be built only from ${extension.mainBranch} branch")
        }

        val supposedVersion = VersionUtils.supposeBranchVersion()
        project.logger.lifecycle("Please specify release version (Should be in x.y format) [$supposedVersion]")


        while (true) {

            var input = readLine()

            if (input == null || input.isBlank()) {
                //Подставляем текущую версию
                input = supposedVersion;
            }


            if (VersionUtils.branchVersionExists(input)) {
                project.logger.lifecycle("Version $input already exists")
                continue
            }

            if (!VersionUtils.isValidBranchVersion(input)) {
                project.logger.lifecycle("Please specify valid version")
                continue
            }

            val branch = "${extension.releaseBranchPrefix}$input"

            if (GitUtils.isBranchExists(branch)) {
                project.logger.lifecycle("Branch with name $branch already exists")
                continue
            }

            GitUtils.createBranch(branch, true)

            project.logger.lifecycle("Branch $branch was successfully created")
            break

        }
    }


}