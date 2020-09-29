package ru.fix.gradle.release.plugin

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
import java.nio.file.Paths

@Disabled("It is required that developer manually provides test data to run this test case.")
@ExtendWith(MockKExtension::class)
class ReleaseTaskOnRemoteReporsitoryManualTest {

    @MockK
    lateinit var project: Project

    @MockK
    lateinit var extensionContainer: ExtensionContainer

    val userInteractor = JournaledTestUserInteractor()

    lateinit var gradlePropertiesFile: Path

    @BeforeEach
    fun beforeEach() {
        mockLogging()
        mockProjectLookup()
        mockProperties()
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
        every{project.projectDir} returns Paths.get("").toAbsolutePath().toFile()
    }

    private fun mockExtension(releaseExtension: ReleaseExtension) {
        every { project.extensions } returns extensionContainer
        every { extensionContainer.findByType(ReleaseExtension::class.java) } returns releaseExtension
    }

    private fun mockProperties() {
        every { project.hasProperty(PluginProperties.GIT_LOGIN.name) } returns false
        every { project.hasProperty(PluginProperties.GIT_PASSWORD.name) } returns false
        every { project.hasProperty(PluginProperties.RELEASE_MAJOR_MINOR_VERSION.name) } returns false
        every { project.hasProperty(PluginProperties.CHECKOUT_TAG.name) } returns false
        every { project.hasProperty(PluginProperties.CREATE_DEFAULT_RELEASE_BRANCH.name) } returns false

        every { project.hasProperty(PluginProperties.DRY_RUN.name) } returns true
        every { project.property(PluginProperties.DRY_RUN.name) } returns true
    }

    private fun mockLogging() {
        userInteractor.clear()
    }

//    @Test
    fun test() {
        BranchGardener(project, userInteractor, ProjectFilesLookup(project, userInteractor)).createRelease()
    }
}