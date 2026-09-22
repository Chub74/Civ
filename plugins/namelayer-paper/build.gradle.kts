plugins {
    alias(libs.plugins.paper.userdev)
    alias(libs.plugins.shadow)
}

version = "3.0.6"

dependencies {
    paperweight {
        paperDevBundle(libs.versions.paper)
    }

    compileOnly(project(":plugins:civmodcore-paper"))
    api(project(":libraries:name-api"))

    testImplementation(libs.bundles.junit)
    testImplementation(libs.mockbukkit)
    testImplementation(libs.paper.api.test)
    testImplementation(libs.mockito.core)
    // GroupManagerDao references civmodcore's ManagedDatasource, which Mockito must load to mock it.
    testImplementation(project(":plugins:civmodcore-paper"))
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