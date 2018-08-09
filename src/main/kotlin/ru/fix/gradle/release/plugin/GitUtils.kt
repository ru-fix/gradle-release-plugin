package ru.fix.gradle.release.plugin

import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import com.jcraft.jsch.agentproxy.RemoteIdentityRepository
import com.jcraft.jsch.agentproxy.connector.SSHAgentConnector
import com.jcraft.jsch.agentproxy.usocket.JNAUSocketFactory
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.transport.*
import org.eclipse.jgit.util.FS
import org.gradle.api.logging.Logging
import ru.fix.gradle.release.plugin.GitHolder.git
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException


class GitUtils {
    companion object {


        const val GIT_LOGIN_PARAMETER = "git.login"
        const val GIT_PASSWORD_PARAMETER = "git.password"


        private val logger = Logging.getLogger("GitUtils")!!

        fun getCurrentBranch(): String = GitHolder.git.repository.branch

        fun isBranchExists(branch: String): Boolean {
            return GitHolder.git.branchList().call()
                    .stream().filter { "refs/heads/$branch" == it.name }
                    .findAny().isPresent
        }

        fun createBranch(branch: String, checkout: Boolean = false): Ref {


            logger.lifecycle("Creating branch $branch ${if (checkout) "and checkout" else ""}")
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

        fun checkoutBranch(branch: String, remote: Boolean) {
            if (!remote) {
                logger.lifecycle("Checkout local branch $branch")
                GitHolder.git.checkout()
                        .setName(branch).call()
            } else {
                logger.lifecycle("Checkout remote branch $branch")
                if (!isBranchExists(branch)) {
                    GitHolder.git.checkout()
                            .setCreateBranch(true)
                            .setName(branch)
                            .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
                            .setStartPoint("origin/$branch")
                            .call()
                } else {
                    GitHolder.git.checkout()
                            .setName(branch).call()
                }
            }

        }

        fun checkoutTag(tag: String) {
            logger.lifecycle("Checkout $tag tag")
            GitHolder.git.checkout()
                    .setCreateBranch(true)
                    .setName("tags/$tag")
                    .call()
        }

        fun fetchTags(){
            logger.lifecycle("Fetching tags")
            GitHolder.git.fetch()
                    .setTagOpt(TagOpt.FETCH_TAGS)
                    .call()
        }

        fun createTag(name: String, comment: String): Ref {

            logger.lifecycle("Creating tag $name with comment $comment")


            return GitHolder.git
                    .tag()
                    .setAnnotated(true)
                    .setName(name)
                    .setMessage(comment)
                    .call()
        }


        fun deleteBranch(name: String) {
            logger.lifecycle("Deleting branch $name")
            GitHolder.git
                    .branchDelete()
                    .setBranchNames(name)
                    .setForce(true)
                    .call()
        }


        fun commitFilesInIndex(commitMessage: String) {
            logger.lifecycle("Committing files")

            GitHolder.git.add().addFilepattern(".")
                    .call()
            git.commit()
                    .setMessage(commitMessage)
                    .call()

        }

        fun pushTag(userName: String, password: String, tagRef: Ref) {
            logger.lifecycle("Pushing tag $tagRef to remote repository")

            val pushCommand = GitHolder.git.push()

            with(pushCommand) {
                add(tagRef)
                setCredentialsProvider(UsernamePasswordCredentialsProvider(userName, password))
                call()
            }

        }

        fun pushTagViaSsh(tagRef: Ref) {

            val sessionFactory = createFactory()
            SshSessionFactory.setInstance(sessionFactory)

            GitHolder.git.push()
                    .setTransportConfigCallback({ transport ->
                        if(transport is SshTransport) {
                            transport.sshSessionFactory = sessionFactory
                        }
                    })
                    .add(tagRef)
                    .call()
        }

        private fun createFactory(): JschConfigSessionFactory {

            return object : JschConfigSessionFactory() {

                override fun configure(host: OpenSshConfig.Host, session: Session) {
                    logger.lifecycle("Configure session")
                    // This can be removed, but the overridden method is required since JschConfigSessionFactory is abstract
                    session.setConfig("StrictHostKeyChecking", "false")
                }

                override fun createDefaultJSch(fs: FS): JSch {
                    var con: SSHAgentConnector? = null

                    if (SSHAgentConnector.isConnectorAvailable()) {
                        val usf = JNAUSocketFactory()
                        con = SSHAgentConnector(usf)
                    }

                    if (con == null) {
                        logger.lifecycle("Using default jsch")
                        return super.createDefaultJSch(fs)
                    } else {
                        logger.lifecycle("Using not-default jsch")
                        val jsch = JSch()
                        JSch.setConfig("PreferredAuthentications", "publickey");
                        val irepo = RemoteIdentityRepository(con);
                        jsch.identityRepository = irepo;
                        knownHosts(jsch, fs) // private method from parent class, yeah for Groovy!
                        return jsch
                    }
                }
            }
        }


        @Throws(JSchException::class)
        private fun knownHosts(sch: JSch, fs: FS) {
            val home = fs.userHome() ?: return
            val known_hosts = File(File(home, ".ssh"), "known_hosts") //$NON-NLS-1$ //$NON-NLS-2$
            try {
                val `in` = FileInputStream(known_hosts)
                try {
                    sch.setKnownHosts(`in`)
                } finally {
                    `in`.close()
                }
            } catch (none: FileNotFoundException) {
                // Oh well. They don't have a known hosts in home.
            } catch (err: IOException) {
                // Oh well. They don't have a known hosts in home.
            }

        }


    }


}