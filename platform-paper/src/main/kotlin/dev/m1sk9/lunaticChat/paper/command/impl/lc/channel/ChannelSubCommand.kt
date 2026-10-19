package dev.m1sk9.lunaticChat.paper.command.impl.lc.channel

import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelRole
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelMembershipManager
import dev.m1sk9.lunaticChat.paper.command.core.LunaticSubCommand
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.CompletableFuture
import com.mojang.brigadier.context.CommandContext as BrigadierCommandContext

private const val MAX_MEMBERS_DISPLAY = 10

/**
 * Renders a channel's member list on one line, truncated to [MAX_MEMBERS_DISPLAY] names with a
 * count of what was left out.
 *
 * @param indent Leading whitespace, which differs by how deeply the caller nests the line
 */
internal fun memberListLine(
    memberNames: List<String>,
    indent: String,
    languageManager: LanguageManager,
): Component {
    val shown = memberNames.take(MAX_MEMBERS_DISPLAY)
    val line =
        Component
            .text(indent)
            .append(Component.text(languageManager.getMessage("channel.info.members"), NamedTextColor.GRAY))
            .append(Component.text(": ", NamedTextColor.GRAY))
            .append(Component.text(shown.joinToString(", "), NamedTextColor.WHITE))

    if (memberNames.size <= MAX_MEMBERS_DISPLAY) return line

    val omitted =
        languageManager.getMessage(
            "channel.info.membersOmitted",
            mapOf("count" to memberNames.size.toString()),
        )
    return line
        .append(Component.text(" ... ", NamedTextColor.GRAY))
        .append(Component.text("($omitted)", NamedTextColor.YELLOW))
}

/**
 * A subcommand of `/lc channel`.
 *
 * The moderation subcommands all open with the same sequence - resolve the sender's active
 * channel, check their role, resolve the named target - once written out in full in each of them.
 * These helpers name each step, and derive their message keys from [literal] so a subcommand
 * cannot accidentally report another one's text.
 */
abstract class ChannelSubCommand(
    plugin: LunaticChat,
    protected val channelManager: ChannelManager,
    protected val membershipManager: ChannelMembershipManager,
) : LunaticSubCommand(plugin) {
    /** A failure carrying `channel.<literal>.<suffix>`. */
    protected fun failHere(
        suffix: String,
        args: Map<String, String> = emptyMap(),
    ): CommandResult = fail("channel.$literal.$suffix", args)

    /** A success carrying `channel.<literal>.<suffix>`. */
    protected fun okHere(
        suffix: String,
        args: Map<String, String> = emptyMap(),
    ): CommandResult = ok("channel.$literal.$suffix", args)

    /** The channel [sender] is currently talking in, or null if they have none. */
    protected fun activeChannelOf(sender: Player): String? = channelManager.getPlayerChannel(sender.uniqueId)

    /** The display name of [channelId], falling back to the id when the channel is gone. */
    protected fun channelNameOf(channelId: String): String = channelManager.getChannel(channelId).getOrNull()?.name ?: channelId

    /**
     * Null when [playerId] holds [role] or higher in [channelId]; otherwise the "no permission"
     * failure for this subcommand.
     */
    protected fun denyUnlessRole(
        playerId: UUID,
        channelId: String,
        role: ChannelRole,
    ): CommandResult? = if (membershipManager.hasRole(playerId, channelId, role).getOrDefault(false)) null else failHere("noPermission")

    /**
     * The named player if the server has ever seen them, or null. Offline players are resolvable
     * because bans and role changes must work while the target is away.
     */
    protected fun knownPlayer(playerName: String): OfflinePlayer? =
        Bukkit.getOfflinePlayer(playerName).takeIf { it.hasPlayedBefore() || it.isOnline }

    /** Suggests the names of the other members of the executor's active channel. */
    protected fun suggestOtherMembers(
        ctx: BrigadierCommandContext<CommandSourceStack>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        val player = ctx.source.executor as? Player ?: return builder.buildFuture()
        val channelId = activeChannelOf(player) ?: return builder.buildFuture()
        channelManager
            .getChannelMembers(channelId)
            .getOrNull()
            .orEmpty()
            .filter { it.playerId != player.uniqueId }
            .forEach { member -> Bukkit.getOfflinePlayer(member.playerId).name?.let(builder::suggest) }
        return builder.buildFuture()
    }
}
