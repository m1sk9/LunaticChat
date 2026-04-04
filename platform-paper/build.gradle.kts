plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.gradleup.shadow")
    id("xyz.jpenilla.run-paper")
    id("org.jetbrains.dokka")
}

version = findProperty("paperVersion")?.toString() ?: "0.0.0"

repositories {
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
}

dependencies {
    // Engine module (provides serialization, coroutines, ktor)
    api(project(":engine"))

    // Paper-specific dependencies
    compileOnly("io.papermc.paper:paper-api:26.1.1.build.16-alpha")
    implementation("com.charleskorn.kaml:kaml:0.104.0") // YAML configuration
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.3.20") // Annotation processing

    // Test dependencies
    testImplementation("io.papermc.paper:paper-api:26.1.1.build.16-alpha")
    testImplementation("io.mockk:mockk:1.14.9")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        archiveBaseName.set("LunaticChat")
    }

    jar {
        enabled = false
    }

    runServer {
        minecraftVersion("26.1.1")
    }

    processResources {
        val gitCommitHash =
            providers
                .exec {
                    commandLine("git", "rev-parse", "--short", "HEAD")
                }.standardOutput
                .asText
                .get()
                .trim()

        val isNightly = providers.gradleProperty("isNightly").orNull?.toBoolean() ?: false
        val props =
            mapOf(
                "version" to project.version,
                "gitCommitHash" to gitCommitHash,
                "channel" to if (isNightly) "nightly" else "stable",
            )
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
        filesMatching("build-info.properties") {
            expand(props)
        }
    }

    build {
        dependsOn(shadowJar)
    }
}
