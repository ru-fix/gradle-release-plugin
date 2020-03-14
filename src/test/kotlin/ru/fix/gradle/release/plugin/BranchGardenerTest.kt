package ru.fix.gradle.release.plugin

import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.withClue
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
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

    @MockK
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
    fun afterEach(){
        releaseProjectLookup()
    }

    private fun releaseProjectLookup() {
        Files.deleteIfExists(gradlePropertiesFile)
    }

    private fun mockProjectLookup() {
        gradlePropertiesFile = Files.createTempFile("gradle.",".properties").apply {
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
        every { project.hasProperty(ProjectProperties.RELEASE_BRANCH_VERSION) } returns false
        every { project.hasProperty(ProjectProperties.CHECKOUT_TAG) } returns false
    }

    private fun mockLogging() {
        userInteractor.clear()
    }

    @Test
    fun `create release with uncommited changes`() {
        every { gitRepo.isUncommittedChangesExist() } returns true
        BranchGardener(project, userInteractor, projectFilesLookup).createRelease()

        withClue(userInteractor.journal){
            userInteractor.journal.any { it.contains("uncommitted changes") }.shouldBeTrue()
        }
    }

    @Test
    fun `create release with wrong current branch`() {
        every { gitRepo.isUncommittedChangesExist() } returns false
        every { gitRepo.fetchTags() } returns Unit
        every { gitRepo.getCurrentBranch() } returns "feature/my-feature-for-1.2"
        BranchGardener(project, userInteractor, projectFilesLookup).createRelease()
        withClue(userInteractor.journal){
            userInteractor.journal.any { it.contains("does not match") }.shouldBeTrue()
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

        BranchGardener(project, userInteractor, projectFilesLookup).createRelease()

        withClue(userInteractor.journal){
            userInteractor.journal.any { it.contains("version 1.2.4") }.shouldBeTrue()
        }
        verify(exactly = 1) { gitRepo.createBranch("temp_gradle_release_plugin/release/1.2.4") }
    }

    private fun mockProperty(name: String, value: Any) {
        every { project.hasProperty(name) } returns true
        every { project.property(name) } returns value
    }

    @Test
    fun `create release branch with uncommited changes`() {
        every { gitRepo.isUncommittedChangesExist() } returns true
        BranchGardener(project, userInteractor, projectFilesLookup).createReleaseBranch()
        withClue(userInteractor.journal){
            userInteractor.journal.any { it.contains("uncommitted changes") }.shouldBeTrue()
        }
    }

    @Test
    fun `create release branch`() {
        BranchGardener(project, userInteractor, projectFilesLookup).createReleaseBranch()
    }
}