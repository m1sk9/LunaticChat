package dev.m1sk9.lunaticChat.paper.common

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.Job
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class HttpClientShutdownTest {
    @Test
    fun `a closed CIO client has finished its engine cleanup once the wait returns`() {
        val client = HttpClient(CIO)
        client.close()

        assertTrue(client.awaitEngineStopped(5.seconds))
        assertEquals(true, client.engine.coroutineContext[Job]?.isCompleted)
    }

    @Test
    fun `the wait gives up after the timeout when the client was never closed`() {
        val client = HttpClient(CIO)
        try {
            assertFalse(client.awaitEngineStopped(50.milliseconds))
        } finally {
            client.close()
        }
    }
}
