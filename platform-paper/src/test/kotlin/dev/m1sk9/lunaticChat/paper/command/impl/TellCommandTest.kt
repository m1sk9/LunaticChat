package dev.m1sk9.lunaticChat.paper.command.impl

import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.PerPlayerWorkQueue
import dev.m1sk9.lunaticChat.paper.TestUtils
import dev.m1sk9.lunaticChat.paper.chat.handler.DirectMessageHandler
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.velocity.CrossServerDirectMessageManager
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlin.test.Test
import kotlin.test.assertIs

class TellCommandTest {
    private fun createCommand(
        crossServerManager: CrossServerDirectMessageManager? = null,
        localServerName: String = "lobby",
        // Unconfined runs the queued delivery inline, so a test can assert on it right after
        // execute() returns.
        scope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined),
    ): TellDeps {
        val plugin = mockk<LunaticChat>(relaxed = true)
        val dmHandler = mockk<DirectMessageHandler>(relaxed = true)
        val languageManager = mockk<LanguageManager>(relaxed = true)
        every { languageManager.getMessage(any()) } returns "msg"
        every { languageManager.getMessage(any(), any()) } returns "msg"

        val sender = TestUtils.createMockPlayer(name = "Alice")
        val ctx = mockk<CommandContext>(relaxed = true)
        every { ctx.requirePlayer() } returns sender

        val command =
            TellCommand(
                plugin,
                dmHandler,
                languageManager,
                crossServerManager,
                null,
                localServerName,
                PerPlayerWorkQueue(scope),
            )
        return TellDeps(command, ctx, dmHandler, crossServerManager, sender)
    }

    private data class TellDeps(
        val command: TellCommand,
        val ctx: CommandContext,
        val dmHandler: DirectMessageHandler,
        val crossServerManager: CrossServerDirectMessageManager?,
        val sender: org.bukkit.entity.Player,
    )

    @Test
    fun `cross-server target fails when cross-server DM is disabled`() {
        val deps = createCommand(crossServerManager = null)

        val result = deps.command.execute(deps.ctx, "Bob@survival", "hello")

        assertIs<CommandResult.Failure>(result)
    }

    @Test
    fun `cross-server target delegates to the manager when enabled`() {
        val manager = mockk<CrossServerDirectMessageManager>(relaxed = true)
        val deps = createCommand(crossServerManager = manager)

        val result = deps.command.execute(deps.ctx, "Bob@survival", "hello")

        assertIs<CommandResult.Success>(result)
        coVerify { manager.sendCrossServerMessage(deps.sender, "Bob", "survival", "hello") }
    }

    @Test
    fun `cross-server target to self on local server is rejected`() {
        val manager = mockk<CrossServerDirectMessageManager>(relaxed = true)
        val deps = createCommand(crossServerManager = manager, localServerName = "lobby")

        // Sender is "Alice", local server is "lobby"
        val result = deps.command.execute(deps.ctx, "Alice@lobby", "hello")

        assertIs<CommandResult.Failure>(result)
        coVerify(exactly = 0) { manager.sendCrossServerMessage(any(), any(), any(), any()) }
    }

    @Test
    fun `cross-server target with empty server part fails`() {
        val manager = mockk<CrossServerDirectMessageManager>(relaxed = true)
        val deps = createCommand(crossServerManager = manager)

        val result = deps.command.execute(deps.ctx, "Bob@", "hello")

        assertIs<CommandResult.Failure>(result)
        coVerify(exactly = 0) { manager.sendCrossServerMessage(any(), any(), any(), any()) }
    }

    @Test
    fun `parseAndExecute splits target and message on first whitespace`() {
        val manager = mockk<CrossServerDirectMessageManager>(relaxed = true)
        val deps = createCommand(crossServerManager = manager)

        val result = deps.command.parseAndExecute(deps.ctx, "Bob@survival hello there")

        assertIs<CommandResult.Success>(result)
        coVerify { manager.sendCrossServerMessage(deps.sender, "Bob", "survival", "hello there") }
    }

    @Test
    fun `parseAndExecute without a message returns usage failure`() {
        val manager = mockk<CrossServerDirectMessageManager>(relaxed = true)
        val deps = createCommand(crossServerManager = manager)

        val result = deps.command.parseAndExecute(deps.ctx, "Bob@survival")

        assertIs<CommandResult.Failure>(result)
        coVerify(exactly = 0) { manager.sendCrossServerMessage(any(), any(), any(), any()) }
    }

    @Test
    fun `execute returns without waiting for delivery`() {
        val dispatcher = StandardTestDispatcher()
        val manager = mockk<CrossServerDirectMessageManager>(relaxed = true)
        val deps = createCommand(crossServerManager = manager, scope = CoroutineScope(dispatcher))

        val result = deps.command.execute(deps.ctx, "Bob@survival", "hello")

        // Delivery can reach the Google IME API; the command must not hold the tick thread for it.
        assertIs<CommandResult.Success>(result)
        coVerify(exactly = 0) { manager.sendCrossServerMessage(any(), any(), any(), any()) }

        dispatcher.scheduler.advanceUntilIdle()
        coVerify { manager.sendCrossServerMessage(deps.sender, "Bob", "survival", "hello") }
    }
}
