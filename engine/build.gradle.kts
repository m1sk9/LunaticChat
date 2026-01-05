plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    // JSON シリアライゼーション
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
}
