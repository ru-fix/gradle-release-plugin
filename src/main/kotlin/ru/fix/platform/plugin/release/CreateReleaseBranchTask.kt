package ru.fix.platform.plugin.release

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

        val supposedVersion = VersionUtils.supposeMajorVersion()
        println("Please specify release version (Should be in x.y format) [$supposedVersion]")


        while (true) {

            var input = readLine()

            if (input == null) {
                //Подставляем текущую версию
                input = supposedVersion;
            }


            if (VersionUtils.majorVersionExists(input)) {
                println("Version $input already exists")
                continue
            }

            if (!VersionUtils.isValidVersion(input)) {
                println("Please specify valid version")
                continue
            }

            val branch = "${extension.releaseBranchPrefix}$input"

            if (GitUtils.isBranchExists(branch)) {
                println("Branch with name $branch already exists")
                continue
            }

            GitUtils.createBranch(branch, true)

            println("Branch $branch was successfully created")
            break

        }
    }


}