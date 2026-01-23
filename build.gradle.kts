import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.3.0" apply false
    kotlin("plugin.serialization") version "2.3.0" apply false
    id("com.gradleup.shadow") version "9.3.1" apply false
    id("xyz.jpenilla.run-paper") version "3.0.2" apply false
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1" apply false
    id("org.jetbrains.dokka") version "2.1.0" apply false
}

allprojects {
    group = "dev.m1sk9"
    version = "1.0.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    // Skip dokka module (aggregator only, no Kotlin compilation needed)
    if (name == "dokka") return@subprojects

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "org.jetbrains.dokka")

    tasks.withType<KotlinCompile> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    configure<DokkaExtension> {
        dokkaSourceSets {
            configureEach {
                sourceLink {
                    remoteUrl("https://github.com/m1sk9/LunaticChat/tree/main")
                    localDirectory.set(rootDir)
                }
            }
        }
    }

    afterEvaluate {
        dependencies {
            // Common test dependencies (applied to all subprojects)
            add("testImplementation", "org.jetbrains.kotlin:kotlin-test-junit5")
            add("testImplementation", "org.junit.jupiter:junit-jupiter-api:5.14.2")
            add("testRuntimeOnly", "org.junit.jupiter:junit-jupiter-engine:5.14.2")
        }
    }
}
