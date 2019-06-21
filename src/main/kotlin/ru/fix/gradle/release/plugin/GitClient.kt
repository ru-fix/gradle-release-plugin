package ru.fix.gradle.release.plugin

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.agentproxy.RemoteIdentityRepository
import com.jcraft.jsch.agentproxy.connector.SSHAgentConnector
import com.jcraft.jsch.agentproxy.usocket.JNAUSocketFactory
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.TransportCommand
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.*
import org.eclipse.jgit.util.FS
import org.gradle.api.GradleException
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logging
import java.io.File
import java.io.FileInputStream


class GitCredentials(val login: String, val password: String)

class GitClient(
        private val credentials: GitCredentials? = null) : AutoCloseable {

    val directory: String?
        get() = git.repository?.directory?.absolutePath

    private val logger = Logging.getLogger(Git::class.simpleName)

    private lateinit var git: Git

    /**
     * search and open existing repo
     * @return false if repo does not exist
     */
    fun find(path: File): Boolean {


        val builder = FileRepositoryBuilder()
                .readEnvironment()
                .findGitDir(path)

        val exist =
                if (builder.gitDir != null || builder.workTree != null) {
                    git = Git(builder.build())
                    git.repository?.objectDatabase?.exists() ?: false
                } else {
                    false
                }
        return exist
    }


    private fun createJschSessionFactory(): JschConfigSessionFactory {

        return object : JschConfigSessionFactory() {

            override fun configure(host: OpenSshConfig.Host, session: Session) {
                logger.lifecycle("Configure session for host: ${host.hostName}")
                session.setConfig("StrictHostKeyChecking", "false")
            }

            override fun createDefaultJSch(fs: FS): JSch {
                var connector: SSHAgentConnector? = null

                if (SSHAgentConnector.isConnectorAvailable()) {
                    val usf = JNAUSocketFactory()
                    connector = SSHAgentConnector(usf)
                }

                if (connector == null) {
                    logger.lifecycle("Using default jsch")
                    return super.createDefaultJSch(fs)

                } else {
                    logger.lifecycle("Using not-default jsch")
                    val jsch = JSch()
                    JSch.setConfig("PreferredAuthentications", "publickey");
                    val irepo = RemoteIdentityRepository(connector)
                    jsch.identityRepository = irepo
                    populateKnownHosts(jsch, fs)
                    return jsch
                }
            }
        }
    }

    private fun populateKnownHosts(sch: JSch, fs: FS) {
        val home = fs.userHome() ?: return
        val knownHostsFile = File(File(home, ".ssh"), "known_hosts")
        if (knownHostsFile.exists()) {
            logger.lifecycle("Found known hosts at: ${knownHostsFile.absoluteFile}")

            try {
                val knownHostsInStream = FileInputStream(knownHostsFile)
                try {
                    sch.setKnownHosts(knownHostsInStream)
                } finally {
                    knownHostsInStream.close()
                }
            } catch (exc: Exception) {
                logger.lifecycle("Failed to initialize known hosts: ${exc.message}", exc)
            }
        }
    }

    private fun setupTransport(command: TransportCommand<*, *>) {
        val sessionFactory = createJschSessionFactory()
        SshSessionFactory.setInstance(sessionFactory)

        if (credentials != null) {
            logger.lifecycle("Pushing on behalf of ${credentials.login}")
            command.setCredentialsProvider(UsernamePasswordCredentialsProvider(credentials.login, credentials.password))
        }
        command.setTransportConfigCallback { transport ->
            if (transport is SshTransport) {
                logger.lifecycle("Configure ssh transport")
                transport.sshSessionFactory = sessionFactory

            } else {
                logger.lifecycle("Using transport: ${transport.javaClass.simpleName}")
            }
        }
    }


    fun fetchTags() {
        logger.lifecycle("Fetching tags")

        try {
            git.fetch().apply {
                setTagOpt(TagOpt.FETCH_TAGS)
                setupTransport(this)
                call()
            }

        } catch (exc: Exception) {
            throw GradleException(
                    "Failed to fetch tags from remote repository.".let {
                        if (credentials == null) "$it\n Be aware that there was no credentials provided." else it
                    }, exc)
        }

        logger.lifecycle("Tags fetched")
    }


    fun listTags(): List<String> {
        return git
                .tagList()
                .call()
                .mapNotNull { it.name }
                .map { it.replace("refs/tags/", "") }
    }


    fun getCurrentBranch(): String = git.repository.branch

    fun checkoutLocalBranch(branch: String) {
        logger.lifecycle("Checkout local branch $branch")
        git.checkout()
                .setName(branch).call()
    }

    fun checkoutRemoteBranch(branch: String) {
        logger.lifecycle("Checkout remote branch $branch")
        git.checkout()
                .setCreateBranch(true)
                .setName(branch)
                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
                .setStartPoint("origin/$branch")
                .call()

    }


    fun pushTag(tagRef: Ref) {

        logger.lifecycle("Pushing tag $tagRef to remote repository")

        try {

            git.push()
                    .apply { setupTransport(this) }
                    .add(tagRef)
                    .call()
        } catch (exc: Exception) {
            logger.lifecycle("Failed to push tag $tagRef to remote repository.\n" +
                    "Release tag is created locally, but not propagated to remote repository.\n" +
                    "You have to manually push changes to remote repository.\n" +
                    "You can use 'git push --tags'\n" +
                    "Push failed due to: ${exc.message}\n" +
                    "You can run gradle with --debug logging level to see details.")
            logger.log(LogLevel.DEBUG, exc.message, exc)
        }
    }

    fun deleteBranch(name: String) {
        logger.lifecycle("Deleting branch $name")
        git
                .branchDelete()
                .setBranchNames(name)
                .setForce(true)
                .call()
    }

    fun checkoutTag(tag: String) {
        logger.lifecycle("Checkout $tag tag")
        git.checkout()
                .setCreateBranch(true)
                .setName("tags/$tag")
                .call()
    }

    fun createTag(name: String, comment: String): Ref {

        logger.lifecycle("Creating tag $name with comment $comment")

        return git
                .tag()
                .setAnnotated(true)
                .setName(name)
                .setMessage(comment)
                .call()
    }

    fun commitFilesInIndex(commitMessage: String) {
        logger.lifecycle("Committing files.")

        git.add().addFilepattern(".")
                .call()
        git.commit()
                .setMessage(commitMessage)
                .call()

    }

    fun createBranch(branch: String, checkout: Boolean = false): Ref {
        logger.lifecycle("Creating branch $branch ${if (checkout) "and checkout" else ""}")
        val ref = git.branchCreate().setName(branch).call()
        return if (checkout) {
            git
                    .checkout()
                    .setName(branch)
                    .call()
        } else {
            ref
        }
    }

    fun isLocalBranchExists(branch: String): Boolean {
        return git.branchList().call()
                .stream().filter { "refs/heads/$branch" == it.name }
                .findAny().isPresent
    }

    fun isUncommittedChangesExist(): Boolean {
        val conflicting = git.status().call().uncommittedChanges
        if (conflicting.isEmpty()) {
            return false
        } else {
            logger.lifecycle("Found uncommited chages: $conflicting")
            return true
        }
    }

    override fun close() {
        if (::git.isInitialized) {
            git.close()
        }
    }
}



