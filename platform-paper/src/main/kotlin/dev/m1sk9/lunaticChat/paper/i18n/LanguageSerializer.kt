package dev.m1sk9.lunaticChat.paper.i18n

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Reads a [Language] from its code, as written in config.yml.
 *
 * Goes through [Language.fromCode] rather than the enum name so the lookup stays
 * case-insensitive and an unrecognised code falls back to English, instead of failing the whole
 * configuration over one typo.
 */
object LanguageSerializer : KSerializer<Language> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Language", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Language = Language.fromCode(decoder.decodeString())

    override fun serialize(
        encoder: Encoder,
        value: Language,
    ) {
        encoder.encodeString(value.code)
    }
}
