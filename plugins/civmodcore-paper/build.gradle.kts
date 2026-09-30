import xyz.jpenilla.runpaper.task.RunServer

plugins {
    alias(libs.plugins.paper.userdev)
    alias(libs.plugins.shadow)
    alias(libs.plugins.runpaper)
}

version = "3.1.0"

repositories {
    maven("https://repo.opencollab.dev/main/")
}

dependencies {
    paperweight {
        paperDevBundle(libs.versions.paper)
    }

    api("com.github.davidmoten:rtree2:0.9.3")
    api(libs.aikar.acf)
    api(libs.aikar.taskchain)
    api(libs.hikaricp)
    api(libs.commons.lang3)
    api(libs.commons.collections4)

    compileOnly(libs.fastutil)
    compileOnly("org.geysermc.floodgate:api:2.2.5-SNAPSHOT")

    testImplementation(libs.bundles.junit)
    testImplementation(libs.mockbukkit)
    testImplementation(libs.paper.api.test)
}

// https://docs.mockbukkit.org/docs/en/user_guide/advanced/paperweight
paperweight {
    addServerDependencyTo = configurations.named(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME).map { setOf(it) }
}

// 26.3 requires Java 25
afterEvaluate {
    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }
    tasks.withType<JavaCompile> {
        options.release = 25
    }
}
