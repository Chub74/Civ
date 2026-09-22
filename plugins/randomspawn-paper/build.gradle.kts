plugins {
    alias(libs.plugins.paper.userdev)
}

version = "3.0.4"

dependencies {
    paperweight {
        paperDevBundle(libs.versions.paper)
    }

    compileOnly(project(":plugins:civmodcore-paper"))
    compileOnly(project(":plugins:bastion-paper"))
    compileOnly(project(":plugins:banstick-paper"))
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