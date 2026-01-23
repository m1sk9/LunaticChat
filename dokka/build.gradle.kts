plugins {
    id("org.jetbrains.dokka")
}

dependencies {
    dokka(project(":engine"))
    dokka(project(":platform-paper"))
    dokka(project(":platform-velocity"))
}

dokka {
    moduleName.set("LunaticChat")

    dokkaPublications.html {
        includes.from("README.md")
    }

    pluginsConfiguration {
        html {
            footerMessage.set("® 2026 m1sk9 - LunaticChat is not affiliated with Mojang Studios or Microsoft.")
        }
    }
}
