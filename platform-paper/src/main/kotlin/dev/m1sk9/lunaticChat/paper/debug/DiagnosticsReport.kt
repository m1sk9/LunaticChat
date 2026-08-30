package dev.m1sk9.lunaticChat.paper.debug

import dev.m1sk9.lunaticChat.engine.debug.DebugState
import dev.m1sk9.lunaticChat.engine.protocol.ProtocolVersion
import dev.m1sk9.lunaticChat.paper.BuildInfo
import dev.m1sk9.lunaticChat.paper.ServiceContainer
import dev.m1sk9.lunaticChat.paper.config.LunaticChatConfiguration
import dev.m1sk9.lunaticChat.paper.storage.writeTextAtomically
import org.bukkit.plugin.Plugin
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * The report `/lc dump` writes, and the rule about what may go in it.
 *
 * It exists to be pasted into a bug report by someone who is not reading it first, so it carries
 * only what the maintainers need to reproduce a problem: versions, which features are on, and how
 * much of each store is populated. No message body, player name or UUID is ever written - those
 * would leave a public issue holding the chat of people who never agreed to that.
 */
class DiagnosticsReport(
    private val plugin: Plugin,
    private val configuration: LunaticChatConfiguration,
    private val services: ServiceContainer,
    private val debugState: DebugState,
    private val directory: Path = plugin.dataFolder.resolve("debug").toPath(),
) {
    private companion object {
        val FILE_TIMESTAMP: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC)
        const val NONE = "(none)"

        /** Matches the guard ChannelMessageLogger puts on its own suffix search. */
        const val MAX_SUFFIX = 1000
    }

    /** Writes the report and returns the file it landed in. */
    fun write(): Path {
        Files.createDirectories(directory)
        val file = unusedFile(FILE_TIMESTAMP.format(Instant.now()))
        file.writeTextAtomically(render())
        return file
    }

    /**
     * The report file for [stamp], suffixed if one is already there.
     *
     * The name is only accurate to the second, and reproducing a bug means dumping either side of
     * the step that triggers it - two dumps a moment apart must not leave one file.
     */
    private fun unusedFile(stamp: String): Path {
        val first = directory.resolve("report-$stamp.txt")
        if (Files.notExists(first)) return first

        for (suffix in 2..MAX_SUFFIX) {
            val candidate = directory.resolve("report-$stamp-$suffix.txt")
            if (Files.notExists(candidate)) return candidate
        }
        return first
    }

    internal fun render(): String =
        buildString {
            appendLine("LunaticChat diagnostics report")
            appendLine("Generated: ${Instant.now()}")
            appendLine()

            section("Build") {
                line("LunaticChat", "${BuildInfo.versionWithCommit()} (${BuildInfo.channel})")
                line(
                    "Protocol",
                    "${ProtocolVersion.version} (accepts minor ${ProtocolVersion.MIN_SUPPORTED_MINOR}..${ProtocolVersion.MINOR})",
                )
            }

            section("Platform") {
                line("Server", "${plugin.server.name} ${plugin.server.version}")
                line("Bukkit API", plugin.server.bukkitVersion)
                line("Java", "${System.getProperty("java.version")} (${System.getProperty("java.vendor")})")
                line("OS", "${System.getProperty("os.name")} ${System.getProperty("os.version")} ${System.getProperty("os.arch")}")
            }

            val features = configuration.features
            section("Features") {
                line("quickReplies", features.quickReplies.enabled.toString())
                line("japaneseConversion", features.japaneseConversion.enabled.toString())
                line("channelChat", features.channelChat.enabled.toString())
                line(
                    "channelChat.messageLogging",
                    features.channelChat.messageLogging.enabled
                        .toString(),
                )
                line("velocityIntegration", features.velocityIntegration.enabled.toString())
                line("crossServerGlobalChat", features.velocityIntegration.crossServerGlobalChat.toString())
                line("crossServerDirectMessage", features.velocityIntegration.crossServerDirectMessage.toString())
                // Cross-server direct messages are routed by this name, so a mismatch with the
                // proxy configuration is the first thing worth ruling out.
                line("serverName", features.velocityIntegration.serverName)
                line("language", configuration.language.code)
            }

            section("Debug") {
                line("categories", debugState.enabled.joinToString(", ") { it.key }.ifEmpty { NONE })
                line(
                    "unknown categories in config.yml",
                    configuration.debug.unknownCategories
                        .joinToString(", ")
                        .ifEmpty { NONE },
                )
            }

            section("Velocity") {
                val velocity = services.velocityConnectionManager
                if (velocity == null) {
                    line("state", "integration disabled")
                } else {
                    line("state", velocity.getState().name)
                    line("proxy plugin version", velocity.getVelocityVersion() ?: NONE)
                    line("last error", velocity.getLastError() ?: NONE)
                }
            }

            section("Stores") {
                val channels = services.channelManager?.getAllChannels()?.getOrNull()
                if (channels == null) {
                    line("channels", "channel chat disabled")
                } else {
                    line("channels", channels.size.toString())
                    line(
                        "channel members (total)",
                        channels
                            .sumOf {
                                services.channelManager
                                    ?.getChannelMembers(it.id)
                                    ?.getOrNull()
                                    ?.size ?: 0
                            }.toString(),
                    )
                }

                val cache = services.conversionCache
                line("conversion cache", if (cache == null) "conversion disabled" else "${cache.entryCount} / ${cache.maxEntries}")
                line("player settings", "${services.playerSettingsManager.trackedPlayerCount} players")
            }

            section("Plugins") {
                plugin.server.pluginManager.plugins.forEach { installed ->
                    line(installed.pluginMeta.name, installed.pluginMeta.version)
                }
            }
        }

    private fun StringBuilder.section(
        title: String,
        body: StringBuilder.() -> Unit,
    ) {
        appendLine("[$title]")
        body()
        appendLine()
    }

    private fun StringBuilder.line(
        label: String,
        value: String,
    ) {
        appendLine("$label: $value")
    }
}
