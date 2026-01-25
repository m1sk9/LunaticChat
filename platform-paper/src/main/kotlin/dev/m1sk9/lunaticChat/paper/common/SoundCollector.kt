package dev.m1sk9.lunaticChat.paper.common

import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.entity.Player

/**
 * Collection of sounds used in LunaticChat.
 */
object SoundCollector {
    val LUNATIC_CLEAR_SOUND: Sound =
        Sound.sound(
            Key.key("block.note_block.chime"),
            Sound.Source.PLAYER,
            0.8f,
            1.5f,
        )

    val LUNATIC_POP_SOUND: Sound =
        Sound.sound(
            Key.key("entity.experience_orb.pickup"),
            Sound.Source.PLAYER,
            0.9f,
            1.4f,
        )

    val LUNATIC_SOFT_SOUND: Sound =
        Sound.sound(
            Key.key("block.note_block.pling"),
            Sound.Source.PLAYER,
            0.6f,
            2.0f,
        )

    val LUNATIC_BELL_SOUND: Sound =
        Sound.sound(
            Key.key("block.note_block.bell"),
            Sound.Source.PLAYER,
            0.7f,
            1.2f,
        )
}

/**
 * Plays the direct message notification sound to the player.
 */
fun Player.playDirectMessageNotification() {
    playSound(SoundCollector.LUNATIC_CLEAR_SOUND)
}

/**
 * Plays the message sent sound to the player.
 */
fun Player.playMessageSendNotification() {
    playSound(SoundCollector.LUNATIC_POP_SOUND)
}

/**
 * Plays the channel join notification sound to the player.
 */
fun Player.playChannelJoinNotification() {
    playSound(SoundCollector.LUNATIC_SOFT_SOUND)
}

/**
 * Plays the channel message receive notification sound to the player.
 */
fun Player.playChannelReceiveNotification() {
    playSound(SoundCollector.LUNATIC_BELL_SOUND)
}
