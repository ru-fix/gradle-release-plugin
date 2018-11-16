package ru.fix.gradle.release.plugin

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.ExtensionContainer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class BranchGardenerTest {

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

        every { project.logger } returns logger


        every { project.hasProperty(ProjectProperties.GIT_LOGIN) } returns false
        every { project.property(ProjectProperties.GIT_LOGIN) } returns null
        every { project.hasProperty(ProjectProperties.GIT_PASSWORD) } returns false
        every { project.property(ProjectProperties.GIT_PASSWORD) } returns null

        every { project.extensions } returns extensionContainer
        every { extensionContainer.findByType(ReleaseExtension::class.java) } returns ReleaseExtension()
    }

    @Test
    fun createRelease() {


        BranchGardener(project).createRelease()

    }

    @Test
    fun createReleaseBranch() {
    }
}