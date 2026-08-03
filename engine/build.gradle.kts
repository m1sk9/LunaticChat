plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    // Exposed to platform modules via api(): the plugin messaging protocol is built on it, so
    // both platforms need it on their compile and runtime classpath.
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
}
