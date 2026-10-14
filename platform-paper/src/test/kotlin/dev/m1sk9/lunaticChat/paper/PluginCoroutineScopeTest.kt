package dev.m1sk9.lunaticChat.paper

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class PluginCoroutineScopeTest {
    @Test
    fun `cancel returns only after cancelled coroutines have finished their cleanup`() {
        val pluginScope = PluginCoroutineScope(TestUtils.TestLogger())
        val started = CountDownLatch(1)
        val cleanedUp = AtomicBoolean(false)
        pluginScope.scope.launch {
            try {
                started.countDown()
                awaitCancellation()
            } finally {
                withContext(NonCancellable) { delay(100) }
                cleanedUp.set(true)
            }
        }
        started.await(5, TimeUnit.SECONDS)

        assertTrue(pluginScope.cancel(5.seconds))
        assertTrue(cleanedUp.get())
    }

    @Test
    fun `cancel gives up after the timeout when a coroutine does not finish`() {
        val pluginScope = PluginCoroutineScope(TestUtils.TestLogger())
        val started = CountDownLatch(1)
        pluginScope.scope.launch {
            try {
                started.countDown()
                awaitCancellation()
            } finally {
                withContext(NonCancellable) { delay(5_000) }
            }
        }
        started.await(5, TimeUnit.SECONDS)

        assertFalse(pluginScope.cancel(50.milliseconds))
    }
}
