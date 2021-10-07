package ru.fix.gradle.release.plugin

//import com.jcraft.jsch.JSch
//import com.jcraft.jsch.Session
//import com.jcraft.jsch.agentproxy.RemoteIdentityRepository
//import com.jcraft.jsch.agentproxy.connector.SSHAgentConnector
//import com.jcraft.jsch.agentproxy.usocket.JNAUSocketFactory
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.TransportCommand
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.*
import org.eclipse.jgit.transport.sshd.SshdSessionFactory
import org.eclipse.jgit.util.FS
import org.gradle.api.GradleException
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logging
import java.io.File
import java.io.FileInputStream

class GitRepositoryNotFound(searchPath: File) :
    Exception("Can not find git repository with search path: $searchPath")

class GitRepository(
    private val credentialsProvider: GitCredentialsProvider,
    private val jGit: Git
) : AutoCloseable {

    companion object {
        fun openExisting(pathUnderGitControl: File, credentialsProvider: GitCredentialsProvider): GitRepository {
            val repoBuilder = FileRepositoryBuilder()
                .readEnvironment()
                .findGitDir(pathUnderGitControl)

            if (repoBuilder.gitDir != null || repoBuilder.workTree != null) {
                val jGit = Git(repoBuilder.build())
                val repositoryExist = jGit.repository?.objectDatabase?.exists() ?: false
                if (repositoryExist) {
                    return GitRepository(credentialsProvider, jGit)
                }
            }
            throw GitRepositoryNotFound(pathUnderGitControl)
        }
    }

    private val logger = Logging.getLogger(GitRepository::class.qualifiedName)

    val directory: String
        get() = jGit.repository!!.directory!!.absolutePath


    private fun createJschSessionFactory(): SshdSessionFactory {
        val factory = SshdSessionFactory()
        return factory;

//
//        return object : SshdSessionFactory() {
//
//            override fun configure(host: OpenSshConfig.Host, session: Session) {
//                logger.lifecycle("Configure session for host: ${host.hostName}")
//                session.setConfig("StrictHostKeyChecking", "false")
//            }
//
//            override fun createDefaultJSch(fs: FS): JSch {
//                var connector: SSHAgentConnector? = null
//
//                if (SSHAgentConnector.isConnectorAvailable()) {
//                    val usf = JNAUSocketFactory()
//                    connector = SSHAgentConnector(usf)
//                }
//
//                if (connector == null) {
//                    logger.lifecycle("Using default jsch")
//                    return super.createDefaultJSch(fs)
//
//                } else {
//                    logger.lifecycle("Using not-default jsch")
//                    val jsch = JSch()
//                    JSch.setConfig("PreferredAuthentications", "publickey");
//                    val irepo = RemoteIdentityRepository(connector)
//                    jsch.identityRepository = irepo
//                    populateKnownHosts(jsch, fs)
//                    return jsch
//                }
//            }
//        }
    }

//    private fun populateKnownHosts(sch: JSch, fs: FS) {
//        val home = fs.userHome() ?: return
//        val knownHostsFile = File(File(home, ".ssh"), "known_hosts")
//        if (knownHostsFile.exists()) {
//            logger.lifecycle("Found known hosts at: ${knownHostsFile.absoluteFile}")
//
//            try {
//                val knownHostsInStream = FileInputStream(knownHostsFile)
//                try {
//                    sch.setKnownHosts(knownHostsInStream)
//                } finally {
//                    knownHostsInStream.close()
//                }
//            } catch (exc: Exception) {
//                logger.lifecycle("Failed to initialize known hosts: ${exc.message}", exc)
//            }
//        }
//    }

    private fun setupTransport(command: TransportCommand<*, *>) {
        val sessionFactory = createJschSessionFactory()
        SshSessionFactory.setInstance(sessionFactory)

        command.setCredentialsProvider(credentialsProvider)

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
            jGit.fetch().apply {
                setTagOpt(TagOpt.FETCH_TAGS)
                setupTransport(this)
                call()
            }

        } catch (exc: Exception) {
            throw GradleException("Failed to fetch tags from remote repository.", exc)
        }

        logger.lifecycle("Tags fetched")
    }


    fun listTags(): List<String> {
        return jGit
            .tagList()
            .call()
            .mapNotNull { it.name }
            .map { it.replace("refs/tags/", "") }
    }


    fun getCurrentBranch(): String = jGit.repository.branch

    fun checkoutLocalBranch(branch: String) {
        logger.lifecycle("Checkout local branch $branch")
        jGit.checkout()
            .setName(branch).call()
    }

    fun checkoutRemoteBranch(branch: String) {
        logger.lifecycle("Checkout remote branch $branch")
        jGit.checkout()
            .setCreateBranch(true)
            .setName(branch)
            .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
            .setStartPoint("origin/$branch")
            .call()

    }


    fun pushTag(tagRef: Ref) {

        logger.lifecycle("Pushing tag $tagRef to remote repository")

        try {

            jGit.push()
                .apply { setupTransport(this) }
                .add(tagRef)
                .call()
        } catch (exc: Exception) {
            logger.lifecycle(
                "Failed to push tag $tagRef to remote repository.\n" +
                        "Release tag is created locally, but not propagated to remote repository.\n" +
                        "You have to manually push changes to remote repository.\n" +
                        "You can use 'git push --tags'\n" +
                        "Push failed due to: ${exc.message}\n" +
                        "You can run gradle with --debug logging level to see details."
            )
            logger.log(LogLevel.DEBUG, exc.message, exc)
        }
    }

    fun deleteBranch(name: String) {
        logger.lifecycle("Deleting branch $name")
        jGit
            .branchDelete()
            .setBranchNames(name)
            .setForce(true)
            .call()
    }

    fun checkoutTag(tag: String) {
        logger.lifecycle("Checkout $tag tag")
        jGit.checkout()
            .setCreateBranch(true)
            .setName("tags/$tag")
            .call()
    }

    fun createTag(name: String, comment: String): Ref {

        logger.lifecycle("Creating tag $name with comment $comment")

        return jGit
            .tag()
            .setAnnotated(true)
            .setName(name)
            .setMessage(comment)
            .call()
    }

    fun commitFilesInIndex(commitMessage: String) {
        logger.lifecycle("Committing files.")

        jGit.add().addFilepattern(".")
            .call()
        jGit.commit()
            .setMessage(commitMessage)
            .call()

    }

    fun createBranch(branch: String, checkout: Boolean = false) {
        logger.lifecycle("Creating branch $branch ${if (checkout) "and checkout" else ""}")
        jGit.branchCreate().setName(branch).call()
        if (checkout) {
            jGit
                .checkout()
                .setName(branch)
                .call()
        }
    }

    fun isLocalBranchExists(branch: String): Boolean {
        return jGit.branchList().call()
            .stream().filter { "refs/heads/$branch" == it.name }
            .findAny().isPresent
    }

    fun isUncommittedChangesExist(): Boolean {
        val conflicting = jGit.status().call().uncommittedChanges
        if (conflicting.isEmpty()) {
            return false
        } else {
            logger.lifecycle("Found uncommited chages: $conflicting")
            return true
        }
    }

    override fun close() {
        jGit.close()
    }
}



