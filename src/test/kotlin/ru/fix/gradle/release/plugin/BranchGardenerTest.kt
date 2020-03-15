package ru.fix.gradle.release.plugin

import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.withClue
import io.kotlintest.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.file.Files
import java.nio.file.Path

@ExtendWith(MockKExtension::class)
class BranchGardenerTest {

    @MockK()
    lateinit var gitRepo: GitRepository

    @MockK
    lateinit var project: Project

    @MockK
    lateinit var extensionContainer: ExtensionContainer

    @MockK
    lateinit var projectFilesLookup: ProjectFilesLookup

    val userInteractor = JournaledTestUserInteractor()

    lateinit var gradlePropertiesFile: Path

    @BeforeEach
    fun beforeEach() {
        mockLogging()
        mockProjectLookup()
        mockAbsentProperties()
        mockDefaultExtension()
    }

    @AfterEach
    fun afterEach() {
        releaseProjectLookup()
    }

    private fun releaseProjectLookup() {
        Files.deleteIfExists(gradlePropertiesFile)
    }

    private fun mockProjectLookup() {
        gradlePropertiesFile = Files.createTempFile("gradle.", ".properties").apply {
            toFile().deleteOnExit()
        }
        every { projectFilesLookup.findGradlePropertiesFile() } returns gradlePropertiesFile
        every { projectFilesLookup.openGitRepository() } returns gitRepo
    }

    private fun mockDefaultExtension() {
        every { project.extensions } returns extensionContainer
        every { extensionContainer.findByType(ReleaseExtension::class.java) } returns ReleaseExtension()
    }

    private fun mockAbsentProperties() {
        every { project.hasProperty(ProjectProperties.GIT_LOGIN) } returns false
        every { project.hasProperty(ProjectProperties.GIT_PASSWORD) } returns false
        every { project.hasProperty(ProjectProperties.RELEASE_BRANCH) } returns false
        every { project.hasProperty(ProjectProperties.CHECKOUT_TAG) } returns false
    }

    private fun mockLogging() {
        userInteractor.clear()
    }

    @Test
    fun `create release with uncommited changes`() {
        every { gitRepo.isUncommittedChangesExist() } returns true

        BranchGardener(project, userInteractor, projectFilesLookup).createRelease()

        verifySequence {
            gitRepo.isUncommittedChangesExist()
        }

        withClue(userInteractor.messages) {
            userInteractor.messages.any { it.contains("uncommitted changes") }.shouldBeTrue()
        }
    }

    @Test
    fun `create release with wrong current branch`() {
        every { gitRepo.isUncommittedChangesExist() } returns false
        every { gitRepo.fetchTags() } returns Unit
        every { gitRepo.getCurrentBranch() } returns "feature/my-feature-for-1.2"

        BranchGardener(project, userInteractor, projectFilesLookup).createRelease()

        verifySequence {
            gitRepo.isUncommittedChangesExist()
            gitRepo.fetchTags()
            gitRepo.getCurrentBranch()
        }

        withClue(userInteractor.messages) {
            userInteractor.messages.any { it.contains("does not match") }.shouldBeTrue()
        }
    }


    @Test
    fun `create release`() {
        every { gitRepo.isUncommittedChangesExist() } returns false
        every { gitRepo.fetchTags() } returns Unit
        every { gitRepo.getCurrentBranch() } returns "release/1.2"
        every { gitRepo.listTags() } returns listOf("1.2.3")
        every { gitRepo.isLocalBranchExists("temp_gradle_release_plugin/release/1.2.4") } returns false
        every { gitRepo.createBranch("temp_gradle_release_plugin/release/1.2.4", true) } returns Unit
        every { gitRepo.commitFilesInIndex("Release v1.2.4") } returns Unit
        every { gitRepo.createTag("1.2.4", "Release v1.2.4") } returns mockk()
        every { gitRepo.checkoutLocalBranch("release/1.2") } returns Unit
        every { gitRepo.deleteBranch("temp_gradle_release_plugin/release/1.2.4") } returns Unit
        every { gitRepo.pushTag(any()) } returns Unit

        BranchGardener(project, userInteractor, projectFilesLookup).createRelease()

        verifySequence {
            gitRepo.isUncommittedChangesExist()
            gitRepo.fetchTags()
            gitRepo.getCurrentBranch()
            gitRepo.listTags()
            gitRepo.isLocalBranchExists("temp_gradle_release_plugin/release/1.2.4")
            gitRepo.createBranch("temp_gradle_release_plugin/release/1.2.4", true)
            gitRepo.commitFilesInIndex("Release v1.2.4")
            gitRepo.createTag("1.2.4", "Release v1.2.4")
            gitRepo.checkoutLocalBranch("release/1.2")
            gitRepo.deleteBranch("temp_gradle_release_plugin/release/1.2.4")
            gitRepo.pushTag(any())
        }

        withClue(userInteractor.messages) {
            userInteractor.messages.any { it.contains("version 1.2.4") }.shouldBeTrue()
        }
    }

    @Test
    fun `create release for single release branch project`() {
        every { gitRepo.isUncommittedChangesExist() } returns false
        every { gitRepo.fetchTags() } returns Unit
        every { gitRepo.getCurrentBranch() } returns "production"
        every { gitRepo.listTags() } returns listOf("1.1.7", "1.2.3")
        every { gitRepo.isLocalBranchExists("temp_gradle_release_plugin/release/1.2.4") } returns false
        every { gitRepo.createBranch("temp_gradle_release_plugin/release/1.2.4", true) } returns Unit
        every { gitRepo.commitFilesInIndex("Release v1.2.4") } returns Unit
        every { gitRepo.createTag("1.2.4", "Release v1.2.4") } returns mockk()
        every { gitRepo.checkoutLocalBranch("production") } returns Unit
        every { gitRepo.deleteBranch("temp_gradle_release_plugin/release/1.2.4") } returns Unit
        every { gitRepo.pushTag(any()) } returns Unit

        BranchGardener(project, userInteractor, projectFilesLookup).createRelease()

        verifySequence {
            gitRepo.isUncommittedChangesExist()
            gitRepo.fetchTags()
            gitRepo.getCurrentBranch()
            gitRepo.listTags()
            gitRepo.isLocalBranchExists("temp_gradle_release_plugin/release/1.2.4")
            gitRepo.createBranch("temp_gradle_release_plugin/release/1.2.4", true)
            gitRepo.commitFilesInIndex("Release v1.2.4")
            gitRepo.createTag("1.2.4", "Release v1.2.4")
            gitRepo.checkoutLocalBranch("production")
            gitRepo.deleteBranch("temp_gradle_release_plugin/release/1.2.4")
            gitRepo.pushTag(any())
        }

        withClue(userInteractor.messages) {
            userInteractor.messages.any { it.contains("version 1.2.4") }.shouldBeTrue()
        }
    }

    private fun mockProperty(name: String, value: Any) {
        every { project.hasProperty(name) } returns true
        every { project.property(name) } returns value
    }

    @Test
    fun `create release branch with uncommited changes`() {
        every { gitRepo.isUncommittedChangesExist() } returns true

        BranchGardener(project, userInteractor, projectFilesLookup).createReleaseBranch()

        verifySequence {
            gitRepo.isUncommittedChangesExist()
        }

        withClue(userInteractor.messages) {
            userInteractor.messages.any { it.contains("uncommitted changes") }.shouldBeTrue()
        }
    }


    @Test
    fun `create release branch from current branch`() {
        every { gitRepo.isUncommittedChangesExist() } returns false
        every { gitRepo.getCurrentBranch() } returns "release/1.2"
        every { gitRepo.listTags() } returns listOf("1.2.0", "1.2.1", "1.1.0", "1.0.0")
        userInteractor.addUserAnswer("1.3")
        every { gitRepo.isLocalBranchExists("release/1.3") } returns false
        every { gitRepo.createBranch("release/1.3", true) } returns Unit

        BranchGardener(project, userInteractor, projectFilesLookup).createReleaseBranch()

        verify { gitRepo.isUncommittedChangesExist() }
        verify { gitRepo.getCurrentBranch() }
        verify { gitRepo.listTags() }
        verify { gitRepo.isLocalBranchExists("release/1.3") }
        verify { gitRepo.createBranch("release/1.3", true) }

        userInteractor.userAnswers.size.shouldBe(0)

        val messages = userInteractor.messages
        withClue(messages) {
            messages.any { it.contains("Default: 1.3") }.shouldBeTrue()
            messages.any { it.contains("release/1.3 was successfully created") }.shouldBeTrue()
            messages.any { it.contains("based on release/1.2") }.shouldBeTrue()
        }
    }

}