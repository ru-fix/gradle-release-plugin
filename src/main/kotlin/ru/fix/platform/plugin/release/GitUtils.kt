package ru.fix.platform.plugin.release

import org.eclipse.jgit.lib.Ref

class GitUtils {
    companion object {


        fun getCurrentBranch(): String = GitHolder.git.repository.branch

        fun isBranchExists(branch: String): Boolean {


            return GitHolder.git.branchList().call()
                    .stream().filter { branch == it.name }
                    .findAny().isPresent
        }

        fun createBranch(branch: String, checkout: Boolean = false): Ref {
            println("Creating branch $branch ${if (checkout) "and checkout" else ""}")
            val ref = GitHolder.git.branchCreate().setName(branch).call()
            return if (checkout) {
                GitHolder.git
                        .checkout()
                        .setName(branch)
                        .call()
            } else {
                ref
            }
        }

        fun checkout(branch: String): Ref {
            println("Checkout to branch $branch")
            return GitHolder.git.checkout().setName(branch).call()
        }

        fun createTag(name: String, comment: String): Ref {

            println("Creating tag $name with comment $comment")


            return GitHolder.git
                    .tag()
                    .setAnnotated(true)
                    .setName(name)
                    .setMessage(comment)
                    .call()
        }


        fun deleteBranch(name: String) {
            println("Deleting branch $name")
            GitHolder.git
                    .branchDelete()
                    .setBranchNames(name)
                    .setForce(true)
                    .call()
        }


    }


}