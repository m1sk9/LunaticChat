package dev.m1sk9.lunaticChat.paper.config

import com.charleskorn.kaml.YamlException
import com.charleskorn.kaml.YamlInput
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A Boolean that also accepts the spellings YAML 1.1 counted as boolean.
 *
 * Bukkit read config.yml as YAML 1.1, where `yes`, `no`, `on` and `off` are booleans; kaml reads
 * YAML 1.2, where they are plain strings. Rejecting them would take a file that has worked for
 * releases and quietly reset the setting - and for `checkForUpdates: no` the default is the
 * opposite of what the file says, so the operator would get behaviour they had turned off.
 */
typealias LenientBoolean =
    @Serializable(with = LenientBooleanSerializer::class)
    Boolean

object LenientBooleanSerializer : KSerializer<Boolean> {
    private val trueWords = setOf("true", "yes", "on", "y")
    private val falseWords = setOf("false", "no", "off", "n")

    // STRING rather than BOOLEAN: the point is to read the scalar before YAML 1.2 decides it is not
    // a boolean at all.
    override val descriptor = PrimitiveSerialDescriptor("LenientBoolean", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Boolean {
        // Captured before decoding: ConfigManager prunes the offending setting by the path its
        // exception carries, and only a YamlException carries one. Throwing without a path would
        // cost the operator every other setting in the file.
        val path = (decoder as YamlInput).node.path
        val raw = decoder.decodeString()
        return when (raw.lowercase()) {
            in trueWords -> true
            in falseWords -> false
            else -> throw YamlException("expected true/false, yes/no or on/off but found '$raw'", path)
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: Boolean,
    ) = encoder.encodeString(value.toString())
}
