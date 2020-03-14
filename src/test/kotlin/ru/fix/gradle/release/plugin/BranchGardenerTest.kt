package ru.fix.gradle.release.plugin

import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.withClue
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
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
    lateinit var git: GitRepository

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
    }

    private fun mockDefaultExtension() {
        every { project.extensions } returns extensionContainer
        every { extensionContainer.findByType(ReleaseExtension::class.java) } returns ReleaseExtension()
    }

    private fun mockAbsentProperties() {
        every { project.hasProperty(ProjectProperties.GIT_LOGIN) } returns false
        every { project.hasProperty(ProjectProperties.GIT_PASSWORD) } returns false
        every { project.hasProperty(ProjectProperties.RELEASE_BRANCH_VERSION) } returns false
    }

    private fun mockLogging() {
        userInteractor.clear()
    }

    @Test
    fun `create release with uncommited changes`() {
        every { git.isUncommittedChangesExist() } returns true
        BranchGardener(project, userInteractor, projectFilesLookup).createRelease()

        withClue(userInteractor.journal){
            userInteractor.journal.any { it.contains("uncommitted changes") }.shouldBeTrue()
        }
    }

    @Test
    fun `create release with wrong current branch`() {
        every { git.isUncommittedChangesExist() } returns false
        every { git.fetchTags() } returns Unit
        every { git.getCurrentBranch() } returns "feature/my-feature-for-1.2"
        BranchGardener(project, userInteractor, projectFilesLookup).createRelease()
        withClue(userInteractor.journal){
            userInteractor.journal.any { it.contains("does not match") }.shouldBeTrue()
        }
    }



    @Test
    fun `create release`() {
        every { git.isUncommittedChangesExist() } returns false
        every { git.fetchTags() } returns Unit
        every { git.getCurrentBranch() } returns "release/1.2"
        every { git.listTags() } returns listOf("1.2.3")
        BranchGardener(project, userInteractor, projectFilesLookup).createRelease()

        withClue(userInteractor.journal){
            userInteractor.journal.any { it.contains("version 1.2.4") }.shouldBeTrue()
        }
    }

    private fun mockProperty(name: String, value: Any) {
        every { project.hasProperty(name) } returns true
        every { project.property(name) } returns value
    }

    @Test
    fun `create release branch with uncommited changes`() {
        every { git.isUncommittedChangesExist() } returns true
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