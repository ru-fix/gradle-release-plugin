package ru.fix.gradle.release.plugin

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.ExtensionContainer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.lang.Exception

@Disabled("Test creates release in current repository, only for manual launching")
@ExtendWith(MockKExtension::class)
class BranchGardenerManualTest {

    @MockK
    lateinit var project: Project

    @MockK
    lateinit var logger: Logger

    @MockK
    lateinit var extensionContainer: ExtensionContainer

    @BeforeEach
    fun init() {
        val msg = slot<String>()
        every { logger.lifecycle(capture(msg)) } answers { println(msg.captured) }

        val debugMsg = slot<String>()
        val debugExc = slot<Throwable>()
        every { logger.log(LogLevel.DEBUG, capture(debugMsg), capture(debugExc)) } answers {
            println(debugMsg.captured)
            println(debugExc.captured)
        }

        every { project.logger } returns logger


        every { project.hasProperty(ProjectProperties.GIT_LOGIN) } returns false
        every { project.property(ProjectProperties.GIT_LOGIN) } returns null
        every { project.hasProperty(ProjectProperties.GIT_PASSWORD) } returns false
        every { project.property(ProjectProperties.GIT_PASSWORD) } returns null
        every { project.hasProperty(ProjectProperties.RELEASE_BRANCH_VERSION) } returns false
        every { project.property(ProjectProperties.RELEASE_BRANCH_VERSION) } returns null

        every { project.extensions } returns extensionContainer
        every { extensionContainer.findByType(ReleaseExtension::class.java) } returns ReleaseExtension()
    }

    @Test
    fun createRelease() {

        every { project.hasProperty(ProjectProperties.CHECKOUT_TAG) } returns true
        every { project.property(ProjectProperties.CHECKOUT_TAG) } returns false

        BranchGardener(project).createRelease()

    }

    @Test
    fun createReleaseBranch() {
        BranchGardener(project).createReleaseBranch()
    }
}