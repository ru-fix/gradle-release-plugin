package ru.fix.gradle.release.plugin

import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.collections.shouldNotBeEmpty
import io.kotlintest.matchers.string.shouldNotBeEmpty
import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.matchers.withClue
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import org.gradle.api.Project
import org.gradle.internal.impldep.com.google.common.io.Files
import org.junit.jupiter.api.Assertions
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
class GitRepositoryManualTest {

    @MockK
    lateinit var project: Project
    @MockK
    lateinit var gradleProjectLogger: org.gradle.api.logging.Logger

    var userInteractor = JournaledTestUserInteractor()

    private val REPOSITORY_PATH = ""

    @BeforeEach
    fun beforeEach(){
        userInteractor.populateAnswersFromFile(
            Paths.get(System.getProperty("user.home"))
                .resolve(Paths.get("workdir/answers.txt")))

        every { project.hasProperty(PluginProperties.GIT_LOGIN.name) } returns false
        every { project.hasProperty(PluginProperties.GIT_PASSWORD.name) } returns false
        every { project.logger } returns gradleProjectLogger
        val messageSlot = slot< String>()
        every { gradleProjectLogger.lifecycle(capture(messageSlot)) }  answers {
            println(messageSlot.captured)
        }

    }

    fun withRepository(block: (GitRepository) -> Unit) =
            GitRepository.openExisting(
                    Paths.get(REPOSITORY_PATH).toAbsolutePath().toFile(),
                    GitCredentialsProvider(project, userInteractor)).use { git ->
        block(git)
    }

    @Test
    fun `git fetch does not throw exception`() = withRepository { git ->
        git.fetchTags()
    }

    @Test
    fun `list tags returns tags`() = withRepository { git ->
        git.fetchTags()

        val tags = git.listTags()
        withClue(tags) {
            tags.shouldNotBeEmpty()
            tags.any { it.contains("1.2") }.shouldBeTrue()
        }
    }

    @Test
    fun `get current branch`() = withRepository { git ->
        git.getCurrentBranch().shouldNotBeNull()
        git.getCurrentBranch().shouldNotBeEmpty()
    }

    @Test
    fun `current branch exist`() = withRepository { git ->
        git.isLocalBranchExists(git.getCurrentBranch()).shouldBeTrue()
    }

    @Test
    fun `check for uncommitted changes does not throw exception`() = withRepository { git ->
        git.isUncommittedChangesExist()
    }

    @Test
    fun `no repository in empty folder`() {
        val dir = Files.createTempDir()
        dir.deleteOnExit()
        Assertions.assertThrows(GitRepositoryNotFound::class.java) {
            GitRepository.openExisting(dir, GitCredentialsProvider(project, userInteractor))
        }
    }
}