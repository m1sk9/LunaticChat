package dev.m1sk9.lunaticChat.paper.config.key

import com.charleskorn.kaml.YamlException
import com.charleskorn.kaml.YamlInput
import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlScalar
import dev.m1sk9.lunaticChat.engine.debug.DebugCategories
import dev.m1sk9.lunaticChat.engine.debug.DebugCategory
import dev.m1sk9.lunaticChat.paper.config.LenientBooleanSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * The `debug` setting, which config.yml may spell as a switch or as a block.
 *
 * @property enabled the master switch. [categories] is ignored while this is false.
 * @property categories the areas that log. Everything, unless config.yml narrows it.
 * @property unknownCategories names config.yml asked for that this build has no category for,
 *   carried rather than thrown so the operator keeps the categories they spelled correctly.
 */
@Serializable(with = DebugConfigSerializer::class)
data class DebugConfig(
    val enabled: Boolean = false,
    val categories: Set<DebugCategory> = DebugCategory.entries.toSet(),
    val unknownCategories: List<String> = emptyList(),
) {
    /** The categories that actually log. */
    val activeCategories: Set<DebugCategory> get() = if (enabled) categories else emptySet()
}

/**
 * Reads `debug` whether it was written as `debug: true` or as a block with `enabled` and
 * `categories`.
 *
 * The node is inspected by hand instead of being decoded through a data class, because one setting
 * has to accept both a scalar and a map and kaml picks the input to build from the descriptor kind
 * alone - a class descriptor rejects `debug: true` before this serializer is ever called.
 */
object DebugConfigSerializer : KSerializer<DebugConfig> {
    // CONTEXTUAL is the one kind kaml accepts for a scalar, a list and a map alike, which is what
    // lets the two spellings share a serializer.
    @OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor("DebugConfig", SerialKind.CONTEXTUAL)

    @Serializable
    private data class DebugBlock(
        val enabled: Boolean,
        val categories: List<String>,
    )

    override fun deserialize(decoder: Decoder): DebugConfig =
        when (val node = (decoder as YamlInput).node) {
            is YamlScalar -> fromSwitch(node)
            is YamlMap -> fromBlock(node)
            // Every YamlException carries the path ConfigManager prunes by, so a debug setting it
            // cannot read costs the operator that setting rather than the whole file.
            else -> throw YamlException("expected true/false or a block with 'enabled' and 'categories'", node.path)
        }

    override fun serialize(
        encoder: Encoder,
        value: DebugConfig,
    ) {
        if (value.categories == DebugCategory.entries.toSet()) {
            encoder.encodeString(value.enabled.toString())
        } else {
            encoder.encodeSerializableValue(
                DebugBlock.serializer(),
                DebugBlock(value.enabled, value.categories.map { it.key }),
            )
        }
    }

    private fun fromSwitch(node: YamlScalar): DebugConfig {
        val parsed = DebugCategories.parse(node.content)
        return DebugConfig(
            enabled = parsed.enabled,
            categories = parsed.categories,
            unknownCategories = parsed.unknown,
        )
    }

    private fun fromBlock(node: YamlMap): DebugConfig {
        val enabled =
            node.get<YamlScalar>("enabled")?.let { scalar ->
                LenientBooleanSerializer.read(scalar.content)
                    ?: throw YamlException("expected true/false, yes/no or on/off but found '${scalar.content}'", scalar.path)
            } ?: false

        val listed = node.get<YamlList>("categories") ?: return DebugConfig(enabled = enabled)
        val names =
            listed.items.map { item ->
                (item as? YamlScalar)?.content
                    ?: throw YamlException("expected a debug category name", item.path)
            }
        val parsed = DebugCategories.resolve(names)

        return DebugConfig(
            enabled = enabled,
            categories = parsed.categories,
            unknownCategories = parsed.unknown,
        )
    }
}
