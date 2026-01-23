plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.gradleup.shadow")
    id("xyz.jpenilla.run-paper")
    id("org.jetbrains.dokka")
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
}

dependencies {
    // Engine module (provides serialization, coroutines, ktor)
    api(project(":engine"))

    // Paper-specific dependencies
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    implementation("com.charleskorn.kaml:kaml:0.104.0") // YAML configuration
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.3.0") // Annotation processing
}

tasks {
    shadowJar {
        archiveClassifier.set("paper")
        archiveBaseName.set("LunaticChat")
    }

    jar {
        enabled = false
    }

    runServer {
        minecraftVersion("1.21")
    }

    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }

    build {
        dependsOn(shadowJar)
    }
}
