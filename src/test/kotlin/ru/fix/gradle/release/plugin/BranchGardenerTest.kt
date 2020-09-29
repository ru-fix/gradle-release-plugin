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
import org.junit.jupiter.api.Assertions
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
        mockExtension(ReleaseExtension())
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

    private fun mockExtension(releaseExtension: ReleaseExtension) {
        every { project.extensions } returns extensionContainer
        every { extensionContainer.findByType(ReleaseExtension::class.java) } returns releaseExtension
    }

    private fun mockAbsentProperties() {
        every { project.hasProperty(PluginProperties.GIT_LOGIN.name) } returns false
        every { project.hasProperty(PluginProperties.GIT_PASSWORD.name) } returns false
        every { project.hasProperty(PluginProperties.RELEASE_MAJOR_MINOR_VERSION.name) } returns false
        every { project.hasProperty(PluginProperties.CHECKOUT_TAG.name) } returns false
        every { project.hasProperty(PluginProperties.CREATE_DEFAULT_RELEASE_BRANCH.name) } returns false
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

        Assertions.assertThrows(BranchDoesNotMatchReleaseBranchNamingConvention::class.java) {
            BranchGardener(project, userInteractor, projectFilesLookup).createRelease()
        }

        verifySequence {
            gitRepo.isUncommittedChangesExist()
            gitRepo.fetchTags()
            gitRepo.getCurrentBranch()
        }
    }


    @Test
    fun `create release`() {
        every { gitRepo.isUncommittedChangesExist() } returns false
        every { gitRepo.fetchTags() } returns Unit
        every { gitRepo.getCurrentBranch() } returns "release/1.2"
        every { gitRepo.listTags() } returns listOf("1.2.3")
        every { gitRepo.isLocalBranchExists("temp_gradle_release_plugin/1.2.4") } returns false
        every { gitRepo.createBranch("temp_gradle_release_plugin/1.2.4", true) } returns Unit
        every { gitRepo.commitFilesInIndex("Release v1.2.4") } returns Unit
        every { gitRepo.createTag("1.2.4", "Release v1.2.4") } returns mockk()
        every { gitRepo.checkoutLocalBranch("release/1.2") } returns Unit
        every { gitRepo.deleteBranch("temp_gradle_release_plugin/1.2.4") } returns Unit
        every { gitRepo.pushTag(any()) } returns Unit

        BranchGardener(project, userInteractor, projectFilesLookup).createRelease()

        verify { gitRepo.isUncommittedChangesExist() }
        verify { gitRepo.fetchTags() }
        verify { gitRepo.getCurrentBranch() }
        verify { gitRepo.isLocalBranchExists("temp_gradle_release_plugin/1.2.4") }
        verify { gitRepo.createBranch("temp_gradle_release_plugin/1.2.4", true) }
        verify { gitRepo.commitFilesInIndex("Release v1.2.4") }
        verify { gitRepo.createTag("1.2.4", "Release v1.2.4") }
        verify { gitRepo.checkoutLocalBranch("release/1.2") }
        verify { gitRepo.deleteBranch("temp_gradle_release_plugin/1.2.4") }
        verify { gitRepo.pushTag(any()) }

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
        every { gitRepo.isLocalBranchExists("temp_gradle_release_plugin/1.2.4") } returns false
        every { gitRepo.createBranch("temp_gradle_release_plugin/1.2.4", true) } returns Unit
        every { gitRepo.commitFilesInIndex("Release v1.2.4") } returns Unit
        every { gitRepo.createTag("1.2.4", "Release v1.2.4") } returns mockk()
        every { gitRepo.checkoutLocalBranch("production") } returns Unit
        every { gitRepo.deleteBranch("temp_gradle_release_plugin/1.2.4") } returns Unit
        every { gitRepo.pushTag(any()) } returns Unit

        mockExtension(ReleaseExtension().apply {
            nextReleaseVersionDeterminationSchema = ReleaseDetection.MAJOR_MINOR_PATCH_FROM_TAG
        })


        BranchGardener(project, userInteractor, projectFilesLookup).createRelease()

        verify { gitRepo.isUncommittedChangesExist() }
        verify { gitRepo.fetchTags() }
        verify { gitRepo.getCurrentBranch() }
        verify { gitRepo.isLocalBranchExists("temp_gradle_release_plugin/1.2.4") }
        verify { gitRepo.createBranch("temp_gradle_release_plugin/1.2.4", true) }
        verify { gitRepo.commitFilesInIndex("Release v1.2.4") }
        verify { gitRepo.createTag("1.2.4", "Release v1.2.4") }
        verify { gitRepo.checkoutLocalBranch("production") }
        verify { gitRepo.deleteBranch("temp_gradle_release_plugin/1.2.4") }
        verify { gitRepo.pushTag(any()) }

        withClue(userInteractor.messages) {
            userInteractor.messages.any { it.contains("version 1.2.4") }.shouldBeTrue()
        }
    }

    @Test
    fun `create release for single release branch project with explicit major minor version`() {
        every { gitRepo.isUncommittedChangesExist() } returns false
        every { gitRepo.fetchTags() } returns Unit
        every { gitRepo.getCurrentBranch() } returns "production"
        every { gitRepo.listTags() } returns listOf("1.1.7", "1.2.3")
        every { gitRepo.isLocalBranchExists("temp_gradle_release_plugin/1.3.0") } returns false
        every { gitRepo.createBranch("temp_gradle_release_plugin/1.3.0", true) } returns Unit
        every { gitRepo.commitFilesInIndex("Release v1.3.0") } returns Unit
        every { gitRepo.createTag("1.3.0", "Release v1.3.0") } returns mockk()
        every { gitRepo.checkoutLocalBranch("production") } returns Unit
        every { gitRepo.deleteBranch("temp_gradle_release_plugin/1.3.0") } returns Unit
        every { gitRepo.pushTag(any()) } returns Unit

        mockExtension(ReleaseExtension().apply {
            nextReleaseVersionDeterminationSchema = ReleaseDetection.MAJOR_MINOR_PATCH_FROM_TAG
        })

        mockProperty("ru.fix.gradle.release.releaseMajorMinorVersion", "1.3")

        BranchGardener(project, userInteractor, projectFilesLookup).createRelease()

        verify { gitRepo.isUncommittedChangesExist() }
        verify { gitRepo.fetchTags() }
        verify { gitRepo.getCurrentBranch() }
        verify { gitRepo.isLocalBranchExists("temp_gradle_release_plugin/1.3.0") }
        verify { gitRepo.createBranch("temp_gradle_release_plugin/1.3.0", true) }
        verify { gitRepo.commitFilesInIndex("Release v1.3.0") }
        verify { gitRepo.createTag("1.3.0", "Release v1.3.0") }
        verify { gitRepo.checkoutLocalBranch("production") }
        verify { gitRepo.deleteBranch("temp_gradle_release_plugin/1.3.0") }
        verify { gitRepo.pushTag(any()) }

        withClue(userInteractor.messages) {
            userInteractor.messages.any { it.contains("version 1.3.0") }.shouldBeTrue()
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

        verify { gitRepo.isUncommittedChangesExist() }

        withClue(userInteractor.messages) {
            userInteractor.messages.any { it.contains("uncommitted changes") }.shouldBeTrue()
        }
    }


    @Test
    fun `create release branch from current branch with user prompt`() {
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

    @Test
    fun `create release branch from current branch without user prompt`() {
        every { project.hasProperty(PluginProperties.CREATE_DEFAULT_RELEASE_BRANCH.name) } returns true
        every { project.property(PluginProperties.CREATE_DEFAULT_RELEASE_BRANCH.name) } returns "true"

        every { gitRepo.isUncommittedChangesExist() } returns false
        every { gitRepo.getCurrentBranch() } returns "release/1.2"
        every { gitRepo.listTags() } returns listOf("1.2.0", "1.2.1", "1.1.0", "1.0.0")

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
            messages.any { it.contains("release/1.3 was successfully created") }.shouldBeTrue()
            messages.any { it.contains("based on release/1.2") }.shouldBeTrue()
        }
    }

}