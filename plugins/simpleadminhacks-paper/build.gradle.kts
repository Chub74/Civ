plugins {
    alias(libs.plugins.paper.userdev)
}

version = "2.3.3"

dependencies {
    paperweight {
        paperDevBundle(libs.versions.paper)
    }

    compileOnly(project(":plugins:civmodcore-paper"))
    compileOnly(project(":plugins:namelayer-paper"))
    compileOnly(project(":plugins:citadel-paper"))
    compileOnly(project(":plugins:combattagplus-paper"))
    compileOnly(project(":plugins:banstick-paper"))
    compileOnly(project(":plugins:bastion-paper"))
    compileOnly(project(":plugins:exilepearl-paper"))
    compileOnly("me.clip:placeholderapi:2.11.6")

    compileOnly(libs.protocollib)
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
