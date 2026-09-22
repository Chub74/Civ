plugins {
    alias(libs.plugins.paper.userdev)
}

version = "3.0.1"

dependencies {
    paperweight {
        paperDevBundle(libs.versions.paper)
    }

    compileOnly(project(":plugins:civmodcore-paper"))
    compileOnly(project(":plugins:namelayer-paper"))
    compileOnly(project(":plugins:citadel-paper"))

    testImplementation(libs.bundles.junit)
    testImplementation(libs.mockito.core)
    testImplementation(project(":plugins:civmodcore-paper"))
    testImplementation(project(":plugins:namelayer-paper"))
    testImplementation(project(":plugins:citadel-paper"))
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