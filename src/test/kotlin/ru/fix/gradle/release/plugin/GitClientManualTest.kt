package ru.fix.gradle.release.plugin

import com.natpryce.hamkrest.anyElement
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import org.eclipse.jgit.lib.Config
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.storage.file.FileBasedConfig
import org.eclipse.jgit.util.FS
import org.eclipse.jgit.util.SystemReader
import org.gradle.internal.impldep.com.google.common.io.Files
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Paths


/**
 * It is required that user manually provides test data to run this test case.
 * - git repository cloned by ssh
 * - git repository cloned by https
 * - with or without credentials
 */
@Disabled("It is required that user manually provides test data to run this test case.")
class GitClientManualTest {

    fun withClient(block: (GitClient) -> Unit) = GitClient().use { git ->
        git.find(Paths.get("").toAbsolutePath().toFile())
        block(git)
    }

    @Test
    fun `git fetch do not throw exception`() = withClient { git ->
        git.fetchTags()
    }

    @Test
    fun `list tags returns tags`() = withClient { git ->

        git.fetchTags()

        assertThat(git.listTags(), List<String>::isNotEmpty)
        assertThat(git.listTags(), anyElement(containsSubstring("1.2")))
    }

    @Test
    fun `get current branch`() = withClient { git ->
        assertNotNull(git.getCurrentBranch())
        assertThat(git.getCurrentBranch(), String::isNotEmpty)
    }

    @Test
    fun `current branch exist`() = withClient { git ->
        assertTrue(git.isLocalBranchExists(git.getCurrentBranch()))
    }

    @Test
    fun `check for uncommitted changes do not throws exception`() = withClient { git ->
        git.isUncommittedChangesExist()
    }

    @Test
    fun `no repository in empty folder`() {
        val dir = Files.createTempDir()
        dir.deleteOnExit()

        GitClient().use {git ->
            assertFalse(git.find(dir))
        }
    }
}