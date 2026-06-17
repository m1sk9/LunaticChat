package dev.m1sk9.lunaticChat.paper

import dev.m1sk9.lunaticChat.paper.velocity.CrossServerDirectMessageManager
import dev.m1sk9.lunaticChat.paper.velocity.RemotePlayerRegistry
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

class ServiceContainerTest {
    @Test
    fun `holds cross-server direct message services and defaults others to null`() {
        val dmManager = mockk<CrossServerDirectMessageManager>()
        val registry = mockk<RemotePlayerRegistry>()

        val container =
            ServiceContainer(
                languageManager = mockk(),
                playerSettingsManager = mockk(),
                directMessageHandler = mockk(),
                crossServerDirectMessageManager = dmManager,
                remotePlayerRegistry = registry,
            )

        assertSame(dmManager, container.crossServerDirectMessageManager)
        assertSame(registry, container.remotePlayerRegistry)
        assertNull(container.velocityConnectionManager)
        assertNull(container.crossServerChatManager)
    }
}
