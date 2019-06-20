package ru.fix.gradle.release.plugin

import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.lib.Config
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.storage.file.FileBasedConfig
import org.eclipse.jgit.util.FS
import org.eclipse.jgit.util.SystemReader
import org.gradle.internal.impldep.com.google.common.io.Files
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows


/**
 * It is required that user manually provides test data to run this test case.
 * - git repository cloned by ssh
 * - git repository cloned by https
 * - with or without credentials
 */
@Disabled("It is required that user manually provides test data to run this test case.")
class GitClientTest {

    @Test
    fun `fetch git`() {
        GitClient().fetchTags()

    }

    @Test
    fun `list tags`() {
        println(GitClient().listTags())
    }

    @Test
    fun `get current branch`() {
        assertNotNull(GitClient().getCurrentBranch())
    }

    @Test
    fun `current branch exist`() {
        GitClient().run {
            assertTrue(isLocalBranchExists(getCurrentBranch()))
        }
    }

    @Test
    fun `check for uncommitted`() {
        println(GitClient().isUncommittedChangesExist())
    }

    @Test
    fun `no repository in empty folder`() {
        val dir = Files.createTempDir()
        dir.deleteOnExit()


        class TestSystemReader(private val default: SystemReader) : SystemReader() {

            // Test environment
            override fun getenv(variable: String?): String? {
                return when(variable){
                    Constants.GIT_DIR_KEY -> dir.absolutePath
                    else -> default.getenv(variable)
                }
            }

            //Other methods do not changes
            override fun getHostname(): String {
                return default.hostname
            }
            override fun getTimezone(`when`: Long): Int {
                return default.getTimezone(`when`)
            }
            override fun openUserConfig(parent: Config?, fs: FS?): FileBasedConfig {
                return default.openUserConfig(parent, fs)
            }
            override fun getProperty(key: String?): String {
                return default.getProperty(key)
            }
            override fun openSystemConfig(parent: Config?, fs: FS?): FileBasedConfig {
                return default.openSystemConfig(parent, fs)
            }
            override fun getCurrentTime(): Long {
                return default.currentTime
            }
        }


        SystemReader.setInstance(TestSystemReader(SystemReader.getInstance()))

        assertThrows<RepositoryNotFoundException> {
            GitClient()
        }
    }
}