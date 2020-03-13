package ru.fix.gradle.release.plugin

import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.collections.shouldNotBeEmpty
import io.kotlintest.matchers.string.shouldNotBeEmpty
import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.matchers.withClue
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.internal.impldep.com.google.common.io.Files
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.file.Paths


/**
 * It is required that developer manually provides test data (git repository) to run this test case.
 * - git repository cloned by ssh
 * - git repository cloned by https
 * - remote repository required or not required credentials
 * - authenticate through ssh key or login/password
 */
@Disabled("It is required that developer manually provides test data to run this test case.")
@ExtendWith(MockKExtension::class)
class GitClientManualTest {

    @MockK
    lateinit var project: Project

    @MockK
    lateinit var logger: Logger

    @MockK
    lateinit var userInteractor: GradleUserInteractor

    @BeforeEach
    fun beforeEach(){
        every { project.hasProperty(ProjectProperties.GIT_LOGIN) } returns false
        every { project.hasProperty(ProjectProperties.GIT_PASSWORD) } returns false

    }

    fun withClient(block: (GitClient) -> Unit) = GitClient(GitCredentialsProvider(project, userInteractor)).use { git ->
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

        val tags = git.listTags()
        withClue(tags) {
            tags.shouldNotBeEmpty()
            tags.any { it.contains("1.2") }.shouldBeTrue()
        }
    }

    @Test
    fun `get current branch`() = withClient { git ->
        git.getCurrentBranch().shouldNotBeNull()
        git.getCurrentBranch().shouldNotBeEmpty()
    }

    @Test
    fun `current branch exist`() = withClient { git ->
        git.isLocalBranchExists(git.getCurrentBranch()).shouldBeTrue()
    }

    @Test
    fun `check for uncommitted changes do not throws exception`() = withClient { git ->
        git.isUncommittedChangesExist()
    }

    @Test
    fun `no repository in empty folder`() {
        val dir = Files.createTempDir()
        dir.deleteOnExit()

        GitClient(GitCredentialsProvider(project, userInteractor)).use { git ->
            assertFalse(git.find(dir))
        }
    }
}