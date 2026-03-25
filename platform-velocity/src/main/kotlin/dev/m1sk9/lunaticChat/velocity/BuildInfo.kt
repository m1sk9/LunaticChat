package dev.m1sk9.lunaticChat.velocity

import java.util.Properties

object BuildInfo {
    val version: String
    val commitHash: String
    val channel: String

    init {
        val props = Properties()
        BuildInfo::class.java.getResourceAsStream("/build-info.properties")?.use {
            props.load(it)
        }
        version = props.getProperty("version", "unknown")
        commitHash = props.getProperty("commit", "unknown")
        channel = props.getProperty("channel", "stable")
    }

    val isNightly: Boolean get() = channel == "nightly"

    fun versionWithCommit(): String = "$version ($commitHash)"
}
