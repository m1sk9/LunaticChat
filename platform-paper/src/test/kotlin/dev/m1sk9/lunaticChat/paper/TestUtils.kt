package dev.m1sk9.lunaticChat.paper

import dev.m1sk9.lunaticChat.engine.chat.channel.Channel
import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelMember
import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelRole
import dev.m1sk9.lunaticChat.engine.settings.PlayerChatSettings
import dev.m1sk9.lunaticChat.paper.config.LunaticChatConfiguration
import dev.m1sk9.lunaticChat.paper.config.key.ChannelChatFeatureConfig
import dev.m1sk9.lunaticChat.paper.config.key.FeaturesConfig
import dev.m1sk9.lunaticChat.paper.config.key.JapaneseConversionFeatureConfig
import dev.m1sk9.lunaticChat.paper.config.key.MessageFormatConfig
import dev.m1sk9.lunaticChat.paper.config.key.QuickRepliesFeatureConfig
import dev.m1sk9.lunaticChat.paper.config.key.VelocityIntegrationConfig
import dev.m1sk9.lunaticChat.paper.i18n.Language
import io.mockk.mockk
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.logging.Logger

/**
 * Common test utilities for LunaticChat tests.
 * Provides mock factories, test data builders, and assertion helpers.
 */
object TestUtils {
    /**
     * Creates a test logger that collects log messages for verification.
     */
    class TestLogger : Logger("test", null) {
        val infoMessages = mutableListOf<String>()
        val warningMessages = mutableListOf<String>()
        val severeMessages = mutableListOf<String>()

        override fun info(msg: String) {
            infoMessages.add(msg)
        }

        override fun warning(msg: String) {
            warningMessages.add(msg)
        }

        override fun severe(msg: String) {
            severeMessages.add(msg)
        }

        fun clear() {
            infoMessages.clear()
            warningMessages.clear()
            severeMessages.clear()
        }
    }

    /**
     * Creates a default test configuration with sensible defaults.
     */
    fun createTestConfiguration(
        quickRepliesEnabled: Boolean = true,
        japaneseConversionEnabled: Boolean = false,
        channelChatEnabled: Boolean = false,
        maxChannelsPerServer: Int = 10,
        maxMembersPerChannel: Int = 50,
        maxMembershipPerPlayer: Int = 5,
        debug: Boolean = false,
        checkForUpdates: Boolean = false,
        language: Language = Language.EN,
    ): LunaticChatConfiguration =
        LunaticChatConfiguration(
            features =
                FeaturesConfig(
                    quickReplies = QuickRepliesFeatureConfig(enabled = quickRepliesEnabled),
                    japaneseConversion =
                        JapaneseConversionFeatureConfig(
                            enabled = japaneseConversionEnabled,
                            cacheMaxEntries = 500,
                            cacheSaveIntervalSeconds = 300,
                            cacheFilePath = "test-conversion-cache.json",
                            apiTimeout = 3000,
                            apiRetryAttempts = 2,
                        ),
                    channelChat =
                        ChannelChatFeatureConfig(
                            enabled = channelChatEnabled,
                            maxChannelsPerServer = maxChannelsPerServer,
                            maxMembersPerChannel = maxMembersPerChannel,
                            maxMembershipPerPlayer = maxMembershipPerPlayer,
                        ),
                    velocityIntegration =
                        VelocityIntegrationConfig(
                            enabled = false,
                        ),
                ),
            messageFormat =
                MessageFormatConfig(
                    directMessageFormat = "§7[§e{sender} §7>> §e{recipient}§7] §f{message}",
                    channelMessageFormat = "§7[§b#{channel}§7] §e{sender}: §f{message}",
                ),
            debug = debug,
            checkForUpdates = checkForUpdates,
            userSettingsFilePath = "test-player-settings.yaml",
            language = language,
        )

    /**
     * Creates a default player settings for testing.
     */
    fun createTestPlayerSettings(
        uuid: UUID = UUID.randomUUID(),
        japaneseConversionEnabled: Boolean = true,
        directMessageNotificationEnabled: Boolean = true,
        channelMessageNotificationEnabled: Boolean = true,
    ): PlayerChatSettings =
        PlayerChatSettings(
            uuid = uuid,
            japaneseConversionEnabled = japaneseConversionEnabled,
            directMessageNotificationEnabled = directMessageNotificationEnabled,
            channelMessageNotificationEnabled = channelMessageNotificationEnabled,
        )

    /**
     * Creates a test channel with default values.
     */
    fun createTestChannel(
        id: String = "test-channel-1",
        name: String = "Test Channel",
        description: String? = null,
        ownerId: UUID = UUID.randomUUID(),
        isPrivate: Boolean = false,
        createdAt: Long = System.currentTimeMillis(),
        bannedPlayers: Set<UUID> = emptySet(),
    ): Channel =
        Channel(
            id = id,
            name = name,
            description = description,
            ownerId = ownerId,
            createdAt = createdAt,
            isPrivate = isPrivate,
            bannedPlayers = bannedPlayers,
        )

    /**
     * Creates a test channel member.
     */
    fun createTestChannelMember(
        channelId: String = "test-channel-1",
        playerId: UUID = UUID.randomUUID(),
        role: ChannelRole = ChannelRole.MEMBER,
        joinedAt: Long = System.currentTimeMillis(),
    ): ChannelMember =
        ChannelMember(
            channelId = channelId,
            playerId = playerId,
            role = role,
            joinedAt = joinedAt,
        )

    /**
     * Creates a mock Player with the given UUID and name.
     */
    fun createMockPlayer(
        uuid: UUID = UUID.randomUUID(),
        name: String = "TestPlayer",
        isOnline: Boolean = true,
    ): Player {
        val player = mockk<Player>(relaxed = true)
        io.mockk.every { player.uniqueId } returns uuid
        io.mockk.every { player.name } returns name
        io.mockk.every { player.isOnline } returns isOnline
        return player
    }

    /**
     * Creates a mock JavaPlugin for testing.
     */
    fun createMockPlugin(): JavaPlugin = mockk<JavaPlugin>(relaxed = true)

    /**
     * Creates a test UUID from an integer for deterministic testing.
     */
    fun createTestUUID(value: Int): UUID =
        UUID.fromString(
            String.format(
                "%08x-0000-0000-0000-000000000000",
                value,
            ),
        )

    /**
     * Assertion helper to check if a string contains all given substrings.
     */
    fun assertContainsAll(
        actual: String,
        vararg expected: String,
    ) {
        expected.forEach { substring ->
            if (!actual.contains(substring)) {
                throw AssertionError("Expected '$actual' to contain '$substring'")
            }
        }
    }

    /**
     * Assertion helper to check if a list contains items matching a predicate.
     */
    fun <T> assertAny(
        list: List<T>,
        predicate: (T) -> Boolean,
    ) {
        if (!list.any(predicate)) {
            throw AssertionError("Expected list to contain at least one matching item")
        }
    }
}
