package dev.m1sk9.lunaticChat.paper

import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServiceShutdownTest {
    private class RecordingService(
        private val name: String,
        private val stopped: MutableList<String>,
        private val failing: Boolean = false,
    ) : StoppableService {
        override fun stop() {
            stopped.add(name)
            if (failing) error("$name could not be stopped")
        }
    }

    private fun shutdown(stoppables: List<StoppableService>): TestUtils.TestLogger {
        val logger = TestUtils.TestLogger()
        val initializer = ServiceInitializer(mockk(relaxed = true), TestUtils.createTestConfiguration(), lazy { mockk() }, logger)
        initializer.shutdown(
            ServiceContainer(
                languageManager = mockk(),
                playerSettingsManager = mockk(),
                directMessageHandler = mockk(),
                stoppables = stoppables,
            ),
        )
        return logger
    }

    @Test
    fun `every service is stopped, in the order it was registered`() {
        val stopped = mutableListOf<String>()

        shutdown(
            listOf(
                RecordingService("settings", stopped),
                RecordingService("cache", stopped),
                RecordingService("velocity", stopped),
            ),
        )

        assertEquals(listOf("settings", "cache", "velocity"), stopped)
    }

    @Test
    fun `a service that fails to stop is reported and does not skip the rest`() {
        val stopped = mutableListOf<String>()

        // An exception escaping onDisable left the log flusher and the proxy connection to be torn
        // down by the server instead of by us.
        val logger =
            shutdown(
                listOf(
                    RecordingService("settings", stopped, failing = true),
                    RecordingService("logger", stopped),
                    RecordingService("velocity", stopped),
                ),
            )

        assertEquals(listOf("settings", "logger", "velocity"), stopped)
        assertTrue(logger.severeMessages.any { it.contains("Failed to stop") })
    }
}
