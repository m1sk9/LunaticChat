package dev.m1sk9.lunaticChat.paper.command.impl.lc

import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.paper.BuildInfo
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.TestUtils
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.config.LunaticChatConfiguration
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.velocity.VelocityConnectionManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import net.kyori.adventure.text.Component
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertIs

class StatusCommandTest {
    private val testUUID = UUID.fromString("00000001-0000-0000-0000-000000000000")

    private fun createDependencies(configuration: LunaticChatConfiguration = TestUtils.createTestConfiguration()): TestDeps {
        val plugin = mockk<LunaticChat>(relaxed = true)
        val languageManager = mockk<LanguageManager>(relaxed = true)
        every { languageManager.getMessage(any(), any()) } returns "test message"

        every { plugin.velocityConnectionManager } returns null

        val mockPlayer = TestUtils.createMockPlayer(uuid = testUUID, name = "Player1")
        val ctx = mockk<CommandContext>(relaxed = true)
        every { ctx.requirePlayer() } returns mockPlayer

        val command = StatusCommand(plugin, languageManager, configuration)
        return TestDeps(command, ctx, plugin, languageManager, configuration, mockPlayer)
    }

    private data class TestDeps(
        val command: StatusCommand,
        val ctx: CommandContext,
        val plugin: LunaticChat,
        val languageManager: LanguageManager,
        val configuration: LunaticChatConfiguration,
        val mockPlayer: org.bukkit.entity.Player,
    )

    @Test
    fun `execute should return Success`() {
        val deps = createDependencies()

        mockkObject(BuildInfo)
        try {
            every { BuildInfo.versionWithCommit() } returns "0.10.0-test"

            val result = deps.command.execute(deps.ctx)

            assertIs<CommandResult.Success>(result)
        } finally {
            unmockkObject(BuildInfo)
        }
    }

    @Test
    fun `execute should send messages to player`() {
        val deps = createDependencies()

        mockkObject(BuildInfo)
        try {
            every { BuildInfo.versionWithCommit() } returns "0.10.0-test"

            deps.command.execute(deps.ctx)

            verify(atLeast = 1) { deps.mockPlayer.sendMessage(any<Component>()) }
        } finally {
            unmockkObject(BuildInfo)
        }
    }

    @Test
    fun `execute with velocity enabled should check connection state`() {
        val velocityConfig =
            TestUtils.createTestConfiguration().let { config ->
                config.copy(
                    features =
                        config.features.copy(
                            velocityIntegration = config.features.velocityIntegration.copy(enabled = true),
                        ),
                )
            }
        val deps = createDependencies(configuration = velocityConfig)

        val velocityManager = mockk<VelocityConnectionManager>(relaxed = true)
        every { velocityManager.getState() } returns VelocityConnectionManager.ConnectionState.CONNECTED
        every { deps.plugin.velocityConnectionManager } returns velocityManager

        mockkObject(BuildInfo)
        try {
            every { BuildInfo.versionWithCommit() } returns "0.10.0-test"

            val result = deps.command.execute(deps.ctx)

            assertIs<CommandResult.Success>(result)
        } finally {
            unmockkObject(BuildInfo)
        }
    }

    @Test
    fun `execute should display feature toggles`() {
        val deps = createDependencies()

        mockkObject(BuildInfo)
        try {
            every { BuildInfo.versionWithCommit() } returns "0.10.0-test"

            deps.command.execute(deps.ctx)

            // Verify multiple sendMessage calls for version, health, features, config, and links
            verify(atLeast = 5) { deps.mockPlayer.sendMessage(any<Component>()) }
        } finally {
            unmockkObject(BuildInfo)
        }
    }
}
