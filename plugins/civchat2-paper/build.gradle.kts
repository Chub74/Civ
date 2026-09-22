plugins {
    alias(libs.plugins.paper.userdev)
}

version = "2.2.2"

dependencies {
    paperweight {
        paperDevBundle(libs.versions.paper)
    }

    compileOnly(project(":plugins:civmodcore-paper"))
    compileOnly(project(":plugins:banstick-paper"))
    compileOnly(project(":plugins:namelayer-paper"))
    compileOnly("me.clip:placeholderapi:2.11.6")
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