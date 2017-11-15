package ru.fix.platform.plugin.release

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.GradleException

class ReleaseUtils {
    companion object {

        fun createGit(): Git {
            val builder = FileRepositoryBuilder()
            val repository = builder.readEnvironment()
                    .findGitDir()
                    .build()
            return Git(repository)
        }

        fun isBranchExists(git: Git, branch: String): Boolean {
            return git.branchList().call()
                    .stream().filter { branch == it.name }
                    .findAny().isPresent
        }

        fun createBranch(git: Git, branch: String): Ref = git.branchCreate().setName(branch).call()



        fun isValidVersion(version: String): Boolean = Regex("(\\d+)\\.(\\d+)").matches(version)



    }




}