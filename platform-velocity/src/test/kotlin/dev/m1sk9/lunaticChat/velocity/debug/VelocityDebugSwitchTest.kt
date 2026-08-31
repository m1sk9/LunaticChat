package dev.m1sk9.lunaticChat.velocity.debug

import dev.m1sk9.lunaticChat.engine.debug.DebugCategory
import io.mockk.mockk
import org.slf4j.Logger
import kotlin.test.Test
import kotlin.test.assertEquals

class VelocityDebugSwitchTest {
    private val logger = mockk<Logger>(relaxed = true)

    private fun read(
        property: String? = null,
        environment: String? = null,
    ) = VelocityDebugSwitch
        .read(logger, property = { property }, environment = { environment })
        .enabled

    @Test
    fun `nothing set logs nothing`() {
        assertEquals(emptySet(), read())
    }

    @Test
    fun `the system property names the categories`() {
        assertEquals(setOf(DebugCategory.VELOCITY, DebugCategory.PROTOCOL), read(property = "velocity,protocol"))
    }

    @Test
    fun `the environment variable is read the same way`() {
        assertEquals(setOf(DebugCategory.VELOCITY, DebugCategory.PROTOCOL), read(environment = "velocity,protocol"))
    }

    @Test
    fun `the system property wins so one run can be debugged without editing the compose file`() {
        assertEquals(setOf(DebugCategory.CHAT), read(property = "chat", environment = "velocity"))
    }

    @Test
    fun `true switches every category on`() {
        assertEquals(DebugCategory.entries.toSet(), read(property = "true"))
    }

    @Test
    fun `an unknown name costs only itself`() {
        assertEquals(setOf(DebugCategory.VELOCITY), read(property = "velocity,proxy"))
    }
}
