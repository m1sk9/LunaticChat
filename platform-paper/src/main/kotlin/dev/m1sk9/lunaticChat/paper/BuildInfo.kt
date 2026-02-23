package dev.m1sk9.lunaticChat.paper

import java.util.Properties

object BuildInfo {
    val version: String
    val commitHash: String

    init {
        val props = Properties()
        BuildInfo::class.java.getResourceAsStream("/build-info.properties")?.use {
            props.load(it)
        }
        version = props.getProperty("version", "unknown")
        commitHash = props.getProperty("commit", "unknown")
    }

    fun versionWithCommit(): String = "$version ($commitHash)"
}
