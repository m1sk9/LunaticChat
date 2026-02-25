package dev.m1sk9.lunaticChat.paper.command.setting.handler

import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.paper.TestUtils
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.command.setting.SettingKey
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.settings.PlayerSettingsManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.kyori.adventure.text.Component
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertIs

class SettingHandlerTest {
    private val testUUID = UUID.fromString("00000001-0000-0000-0000-000000000000")

    private fun createDependencies(): Triple<CommandContext, PlayerSettingsManager, LanguageManager> {
        val player = TestUtils.createMockPlayer(uuid = testUUID, name = "TestPlayer")
        val ctx = mockk<CommandContext>(relaxed = true)
        every { ctx.requirePlayer() } returns player

        val settingsManager = mockk<PlayerSettingsManager>(relaxed = true)
        every { settingsManager.getSettings(testUUID) } returns
            TestUtils.createTestPlayerSettings(uuid = testUUID)

        val languageManager = mockk<LanguageManager>(relaxed = true)
        every { languageManager.getMessage(any(), any()) } returns "test message"
        every { languageManager.getToggleText(any()) } returns "ON"

        return Triple(ctx, settingsManager, languageManager)
    }

    // --- JapaneseConversionSettingHandler ---

    @Test
    fun `JapaneseConversionSettingHandler key should be Japanese`() {
        val (_, settingsManager, languageManager) = createDependencies()
        val handler = JapaneseConversionSettingHandler(settingsManager, languageManager)
        assertIs<SettingKey.Japanese>(handler.key)
    }

    @Test
    fun `JapaneseConversionSettingHandler execute enable should return Success`() {
        val (ctx, settingsManager, languageManager) = createDependencies()
        val handler = JapaneseConversionSettingHandler(settingsManager, languageManager)

        val result = handler.execute(ctx, true)

        assertIs<CommandResult.Success>(result)
        verify { settingsManager.updateSettings(match { it.japaneseConversionEnabled }) }
    }

    @Test
    fun `JapaneseConversionSettingHandler execute disable should return Success`() {
        val (ctx, settingsManager, languageManager) = createDependencies()
        val handler = JapaneseConversionSettingHandler(settingsManager, languageManager)

        val result = handler.execute(ctx, false)

        assertIs<CommandResult.Success>(result)
        verify { settingsManager.updateSettings(match { !it.japaneseConversionEnabled }) }
    }

    @Test
    fun `JapaneseConversionSettingHandler showStatus should return Success`() {
        val (ctx, settingsManager, languageManager) = createDependencies()
        val handler = JapaneseConversionSettingHandler(settingsManager, languageManager)

        val result = handler.showStatus(ctx)

        assertIs<CommandResult.Success>(result)
    }

    @Test
    fun `JapaneseConversionSettingHandler execute should send message to player`() {
        val (ctx, settingsManager, languageManager) = createDependencies()
        val handler = JapaneseConversionSettingHandler(settingsManager, languageManager)

        handler.execute(ctx, true)

        val player = ctx.requirePlayer()
        verify { player.sendMessage(any<Component>()) }
    }

    // --- DirectMessageNoticeSettingHandler ---

    @Test
    fun `DirectMessageNoticeSettingHandler key should be Notice`() {
        val (_, settingsManager, languageManager) = createDependencies()
        val handler = DirectMessageNoticeSettingHandler(settingsManager, languageManager)
        assertIs<SettingKey.Notice>(handler.key)
    }

    @Test
    fun `DirectMessageNoticeSettingHandler execute enable should return Success`() {
        val (ctx, settingsManager, languageManager) = createDependencies()
        val handler = DirectMessageNoticeSettingHandler(settingsManager, languageManager)

        val result = handler.execute(ctx, true)

        assertIs<CommandResult.Success>(result)
        verify { settingsManager.updateSettings(match { it.directMessageNotificationEnabled }) }
    }

    @Test
    fun `DirectMessageNoticeSettingHandler execute disable should return Success`() {
        val (ctx, settingsManager, languageManager) = createDependencies()
        val handler = DirectMessageNoticeSettingHandler(settingsManager, languageManager)

        val result = handler.execute(ctx, false)

        assertIs<CommandResult.Success>(result)
        verify { settingsManager.updateSettings(match { !it.directMessageNotificationEnabled }) }
    }

    @Test
    fun `DirectMessageNoticeSettingHandler showStatus should return Success`() {
        val (ctx, settingsManager, languageManager) = createDependencies()
        val handler = DirectMessageNoticeSettingHandler(settingsManager, languageManager)

        val result = handler.showStatus(ctx)

        assertIs<CommandResult.Success>(result)
    }

    @Test
    fun `DirectMessageNoticeSettingHandler execute should send message to player`() {
        val (ctx, settingsManager, languageManager) = createDependencies()
        val handler = DirectMessageNoticeSettingHandler(settingsManager, languageManager)

        handler.execute(ctx, true)

        val player = ctx.requirePlayer()
        verify { player.sendMessage(any<Component>()) }
    }

    // --- ChannelMessageNoticeSettingHandler ---

    @Test
    fun `ChannelMessageNoticeSettingHandler key should be ChNotice`() {
        val (_, settingsManager, languageManager) = createDependencies()
        val handler = ChannelMessageNoticeSettingHandler(settingsManager, languageManager)
        assertIs<SettingKey.ChNotice>(handler.key)
    }

    @Test
    fun `ChannelMessageNoticeSettingHandler execute enable should return Success`() {
        val (ctx, settingsManager, languageManager) = createDependencies()
        val handler = ChannelMessageNoticeSettingHandler(settingsManager, languageManager)

        val result = handler.execute(ctx, true)

        assertIs<CommandResult.Success>(result)
        verify { settingsManager.updateSettings(match { it.channelMessageNotificationEnabled }) }
    }

    @Test
    fun `ChannelMessageNoticeSettingHandler execute disable should return Success`() {
        val (ctx, settingsManager, languageManager) = createDependencies()
        val handler = ChannelMessageNoticeSettingHandler(settingsManager, languageManager)

        val result = handler.execute(ctx, false)

        assertIs<CommandResult.Success>(result)
        verify { settingsManager.updateSettings(match { !it.channelMessageNotificationEnabled }) }
    }

    @Test
    fun `ChannelMessageNoticeSettingHandler showStatus should return Success`() {
        val (ctx, settingsManager, languageManager) = createDependencies()
        val handler = ChannelMessageNoticeSettingHandler(settingsManager, languageManager)

        val result = handler.showStatus(ctx)

        assertIs<CommandResult.Success>(result)
    }

    @Test
    fun `ChannelMessageNoticeSettingHandler execute should send message to player`() {
        val (ctx, settingsManager, languageManager) = createDependencies()
        val handler = ChannelMessageNoticeSettingHandler(settingsManager, languageManager)

        handler.execute(ctx, true)

        val player = ctx.requirePlayer()
        verify { player.sendMessage(any<Component>()) }
    }
}
