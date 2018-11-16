package ru.fix.gradle.release.plugin

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test


/**
 * It is required that user manually provides test data to run this test case.
 * - git repository cloned by ssh
 * - git repository cloned by https
 * - with or without credentials
 */
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
    fun `check for uncommitted`(){
        println(GitClient().isUncommittedChangesExist())
    }
}