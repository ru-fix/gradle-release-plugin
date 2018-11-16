package ru.fix.gradle.release.plugin

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class BranchGardenerTest {

    @MockK
    lateinit var project: Project

    @MockK
    lateinit var logger: Logger

    @BeforeEach
    fun init() {
        val msg = slot<String>()
        every { logger.lifecycle(capture(msg)) } answers { println(msg.captured) }

        every { project.logger } returns logger

        every { project.property(GIT_LOGIN_PARAMETER) } returns null
        every { project.hasProperty(GIT_LOGIN_PARAMETER) } returns false
        every { project.property(GIT_PASSWORD_PARAMETER) } returns null
        every { project.hasProperty(GIT_PASSWORD_PARAMETER) } returns false

    }

    @Test
    fun createRelease() {


        BranchGardener(project).createRelease()

    }

    @Test
    fun createReleaseBranch() {
    }
}