package dev.m1sk9.lunaticChat.paper.command.impl.lc.chatmode

import dev.m1sk9.lunaticChat.engine.chat.ChatMode
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.TestUtils
import dev.m1sk9.lunaticChat.paper.chat.ChatModeManager
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.kyori.adventure.text.Component
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertIs

class ChatModeToggleCommandTest {
    private val testUUID = UUID.fromString("00000001-0000-0000-0000-000000000000")

    private fun createDependencies(): TestDeps {
        val plugin = mockk<LunaticChat>(relaxed = true)
        val chatModeManager = mockk<ChatModeManager>(relaxed = true)
        val languageManager = mockk<LanguageManager>(relaxed = true)
        every { languageManager.getMessage(any(), any()) } returns "test message"

        val mockPlayer = TestUtils.createMockPlayer(uuid = testUUID, name = "Player1")
        val ctx = mockk<CommandContext>(relaxed = true)
        every { ctx.requirePlayer() } returns mockPlayer

        val command = ChatModeToggleCommand(plugin, chatModeManager, languageManager)
        return TestDeps(command, ctx, chatModeManager, languageManager, mockPlayer)
    }

    private data class TestDeps(
        val command: ChatModeToggleCommand,
        val ctx: CommandContext,
        val chatModeManager: ChatModeManager,
        val languageManager: LanguageManager,
        val mockPlayer: org.bukkit.entity.Player,
    )

    @Test
    fun `execute should toggle to CHANNEL mode`() {
        val deps = createDependencies()

        every { deps.chatModeManager.toggleChatMode(testUUID) } returns ChatMode.CHANNEL

        val result = deps.command.execute(deps.ctx)

        assertIs<CommandResult.Success>(result)
        verify { deps.chatModeManager.toggleChatMode(testUUID) }
    }

    @Test
    fun `execute should toggle to GLOBAL mode`() {
        val deps = createDependencies()

        every { deps.chatModeManager.toggleChatMode(testUUID) } returns ChatMode.GLOBAL

        val result = deps.command.execute(deps.ctx)

        assertIs<CommandResult.Success>(result)
        verify { deps.chatModeManager.toggleChatMode(testUUID) }
    }

    @Test
    fun `execute should send message to player`() {
        val deps = createDependencies()

        every { deps.chatModeManager.toggleChatMode(testUUID) } returns ChatMode.CHANNEL

        deps.command.execute(deps.ctx)

        verify { deps.mockPlayer.sendMessage(any<Component>()) }
    }

    @Test
    fun `execute should return Success`() {
        val deps = createDependencies()

        every { deps.chatModeManager.toggleChatMode(testUUID) } returns ChatMode.GLOBAL

        val result = deps.command.execute(deps.ctx)

        assertIs<CommandResult.Success>(result)
    }
}
