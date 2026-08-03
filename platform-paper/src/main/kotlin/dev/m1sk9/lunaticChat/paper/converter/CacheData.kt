package dev.m1sk9.lunaticChat.paper.converter

import kotlinx.serialization.Serializable

@Serializable
data class CacheData(
    val version: String,
    val entries: Map<String, String>,
)
