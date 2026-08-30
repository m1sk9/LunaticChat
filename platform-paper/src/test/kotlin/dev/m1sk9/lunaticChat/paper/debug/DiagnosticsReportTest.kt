package dev.m1sk9.lunaticChat.paper.debug

import dev.m1sk9.lunaticChat.engine.debug.DebugCategory
import dev.m1sk9.lunaticChat.engine.debug.DebugState
import dev.m1sk9.lunaticChat.paper.ServiceContainer
import dev.m1sk9.lunaticChat.paper.TestUtils
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.chat.handler.DirectMessageHandler
import dev.m1sk9.lunaticChat.paper.converter.ConversionCache
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.settings.PlayerSettingsManager
import dev.m1sk9.lunaticChat.paper.velocity.VelocityConnectionManager
import io.mockk.every
import io.mockk.mockk
import org.bukkit.plugin.Plugin
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiagnosticsReportTest {
    private val playerId = UUID.fromString("4f1c9a3e-0000-0000-0000-0000000000ff")
    private val channel = TestUtils.createTestChannel(id = "secret-plans", name = "Secret Plans")

    private val channelManager =
        mockk<ChannelManager>(relaxed = true) {
            every { getAllChannels() } returns Result.success(listOf(channel))
            every { getChannelMembers(channel.id) } returns
                Result.success(
                    listOf(TestUtils.createTestChannelMember(channelId = channel.id, playerId = playerId)),
                )
        }

    private val services =
        ServiceContainer(
            languageManager = mockk<LanguageManager>(relaxed = true),
            playerSettingsManager = mockk<PlayerSettingsManager>(relaxed = true) { every { trackedPlayerCount } returns 7 },
            directMessageHandler = mockk<DirectMessageHandler>(relaxed = true),
            conversionCache =
                mockk<ConversionCache>(relaxed = true) {
                    every { entryCount } returns 41
                    every { maxEntries } returns 500
                },
            channelManager = channelManager,
            velocityConnectionManager =
                mockk<VelocityConnectionManager>(relaxed = true) {
                    every { getState() } returns VelocityConnectionManager.ConnectionState.CONNECTED
                    every { getVelocityVersion() } returns "1.2.0"
                    every { getLastError() } returns null
                },
        )

    private fun report(debugState: DebugState = DebugState()) =
        DiagnosticsReport(
            plugin = mockk<Plugin>(relaxed = true),
            configuration = TestUtils.createTestConfiguration(),
            services = services,
            debugState = debugState,
        ).render()

    @Test
    fun `the report carries the versions a bug report is triaged by`() {
        val rendered = report()

        assertTrue(rendered.contains("LunaticChat:"))
        assertTrue(rendered.contains("Protocol:"))
        assertTrue(rendered.contains("Java:"))
        assertTrue(rendered.contains("OS:"))
    }

    @Test
    fun `the report says which features are on and which server it came from`() {
        val rendered = report()

        assertTrue(rendered.contains("channelChat:"))
        assertTrue(rendered.contains("velocityIntegration:"))
        // Cross-server routing is keyed on this, so a report without it cannot be triaged.
        assertTrue(rendered.contains("serverName:"))
    }

    @Test
    fun `the report says which debug categories are logging`() {
        val rendered = report(DebugState(setOf(DebugCategory.VELOCITY)))

        assertTrue(rendered.contains("categories: velocity"))
    }

    @Test
    fun `the report counts what each store holds`() {
        val rendered = report()

        assertTrue(rendered.contains("channels: 1"))
        assertTrue(rendered.contains("channel members (total): 1"))
        assertTrue(rendered.contains("conversion cache: 41 / 500"))
        assertTrue(rendered.contains("player settings: 7 players"))
    }

    @Test
    fun `the report names no player, channel or message`() {
        // It exists to be pasted into a public issue, so nothing that identifies a person may reach
        // it - not a UUID, not a name, and not a channel someone created.
        val rendered = report()

        assertFalse(rendered.contains(playerId.toString()), rendered)
        assertFalse(rendered.contains(channel.id), rendered)
        assertFalse(rendered.contains(channel.name), rendered)
    }

    @Test
    fun `the report says so when a feature is off rather than leaving a hole`() {
        val bare =
            DiagnosticsReport(
                plugin = mockk<Plugin>(relaxed = true),
                configuration = TestUtils.createTestConfiguration(),
                services =
                    ServiceContainer(
                        languageManager = mockk(relaxed = true),
                        playerSettingsManager = mockk(relaxed = true),
                        directMessageHandler = mockk(relaxed = true),
                    ),
                debugState = DebugState(),
            ).render()

        assertTrue(bare.contains("state: integration disabled"))
        assertTrue(bare.contains("channels: channel chat disabled"))
        assertTrue(bare.contains("conversion cache: conversion disabled"))
    }
}
