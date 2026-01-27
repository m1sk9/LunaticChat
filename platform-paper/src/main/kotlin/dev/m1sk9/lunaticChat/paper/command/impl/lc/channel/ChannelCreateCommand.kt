package dev.m1sk9.lunaticChat.paper.command.impl.lc.channel

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.chat.channel.Channel
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.exception.ChannelLimitExceededException
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.command.annotation.Permission
import dev.m1sk9.lunaticChat.paper.command.annotation.PlayerOnly
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.command.core.LunaticCommand
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.i18n.MessageFormatter
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands

@PlayerOnly
class ChannelCreateCommand(
    plugin: LunaticChat,
    private val channelManager: ChannelManager,
    private val languageManager: LanguageManager,
) : LunaticCommand(plugin) {
    fun buildWithPermissionCheck(): LiteralArgumentBuilder<CommandSourceStack> {
        val builder = build()
        return applyMethodPermission("build", builder)
    }

    @Permission(LunaticChatPermissionNode.ChannelCreate::class)
    fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        Commands
            .literal("create")
            .then(
                Commands
                    .argument("channelId", StringArgumentType.word())
                    .then(
                        Commands
                            .argument("name", StringArgumentType.string())
                            .executes { ctx ->
                                val context = wrapContext(ctx)
                                checkPlayerOnly(context)?.let { return@executes handleResult(context, it) }

                                val channelId = StringArgumentType.getString(ctx, "channelId")
                                val name = StringArgumentType.getString(ctx, "name")

                                val result = execute(context, channelId, name, null, false)
                                handleResult(context, result)
                            }.then(
                                Commands
                                    .argument("description", StringArgumentType.string())
                                    .executes { ctx ->
                                        val context = wrapContext(ctx)
                                        checkPlayerOnly(context)?.let { return@executes handleResult(context, it) }

                                        val channelId = StringArgumentType.getString(ctx, "channelId")
                                        val name = StringArgumentType.getString(ctx, "name")
                                        val description = StringArgumentType.getString(ctx, "description")

                                        val result = execute(context, channelId, name, description, false)
                                        handleResult(context, result)
                                    }.then(
                                        Commands
                                            .argument("isPrivate", BoolArgumentType.bool())
                                            .executes { ctx ->
                                                val context = wrapContext(ctx)
                                                checkPlayerOnly(context)?.let { return@executes handleResult(context, it) }

                                                val channelId = StringArgumentType.getString(ctx, "channelId")
                                                val name = StringArgumentType.getString(ctx, "name")
                                                val description = StringArgumentType.getString(ctx, "description")
                                                val isPrivate = BoolArgumentType.getBool(ctx, "isPrivate")

                                                val result = execute(context, channelId, name, description, isPrivate)
                                                handleResult(context, result)
                                            },
                                    ),
                            ),
                    ),
            )

    private fun execute(
        ctx: CommandContext,
        channelId: String,
        name: String,
        description: String?,
        isPrivate: Boolean,
    ): CommandResult {
        val sender = ctx.requirePlayer()

        // Validate channel ID pattern
        if (!channelId.matches(Channel.CHANNEL_ID_PATTERN)) {
            return CommandResult.Failure(
                MessageFormatter.formatError(
                    languageManager.getMessage(
                        "channel.create.invalidId",
                        mapOf("id" to channelId),
                    ),
                ),
            )
        }

        val channel =
            Channel(
                id = channelId,
                name = name,
                description = description,
                isPrivate = isPrivate,
                ownerId = sender.uniqueId,
            )

        val result = channelManager.createChannel(channel)
        return result.fold(
            onSuccess = {
                val successMessage =
                    languageManager.getMessage(
                        "channel.create.success",
                        mapOf("name" to name, "id" to channelId),
                    )

                val message =
                    if (isPrivate) {
                        val privateNotice = languageManager.getMessage("channel.create.privateNotice")
                        "$successMessage\n$privateNotice"
                    } else {
                        successMessage
                    }

                CommandResult.SuccessWithMessage(
                    MessageFormatter.format(message),
                )
            },
            onFailure = { error ->
                val messageKey =
                    when (error) {
                        is ChannelLimitExceededException ->
                            "channel.create.limitExceeded"
                        else ->
                            "channel.create.alreadyExists"
                    }
                val params =
                    when (error) {
                        is ChannelLimitExceededException ->
                            mapOf("limit" to error.limit.toString())
                        else ->
                            mapOf("id" to channelId)
                    }
                CommandResult.Failure(
                    MessageFormatter.formatError(
                        languageManager.getMessage(messageKey, params),
                    ),
                )
            },
        )
    }

    override fun buildCommand(): LiteralArgumentBuilder<CommandSourceStack> =
        throw UnsupportedOperationException(
            "ChannelCreateCommand should use build() method instead of buildCommand()",
        )
}
