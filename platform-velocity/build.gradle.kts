plugins {
    kotlin("jvm")
    id("com.gradleup.shadow")
    kotlin("kapt")
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
}

dependencies {
    // engine モジュールを使用
    api(project(":engine"))

    // Velocity API (将来追加予定)
    // compileOnly("com.velocitypowered:velocity-api:3.4.0")
    // kapt("com.velocitypowered:velocity-api:3.4.0")
}

tasks {
    shadowJar {
        archiveClassifier.set("all")
        archiveBaseName.set("LunaticChat-Velocity")
    }

    build {
        dependsOn(shadowJar)
    }
}
