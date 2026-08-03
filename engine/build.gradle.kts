plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    // Exposed to platform modules via api(): the plugin messaging protocol is built on it, so
    // both platforms need it on their compile and runtime classpath.
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    // Adventure API (provided by platform implementations)
    // Matches what every supported platform ships: Paper 26.2 and Velocity 4.x both bundle 5.2.0.
    compileOnly("net.kyori:adventure-api:5.2.0")

    testImplementation("net.kyori:adventure-api:5.2.0")
}
