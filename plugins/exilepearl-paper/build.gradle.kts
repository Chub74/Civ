plugins {
    alias(libs.plugins.paper.userdev)
}

version = "2.1.6"

dependencies {
    paperweight {
        paperDevBundle(libs.versions.paper)
    }

    compileOnly(project(":plugins:civmodcore-paper"))
    compileOnly(project(":plugins:namelayer-paper"))
    compileOnly(project(":plugins:citadel-paper"))
    compileOnly(project(":plugins:civchat2-paper"))
    compileOnly(project(":plugins:bastion-paper"))
    compileOnly(project(":plugins:combattagplus-paper"))
    compileOnly(project(":plugins:banstick-paper"))
    compileOnly(project(":plugins:randomspawn-paper"))

    compileOnly(files("../../ansible/src/paper-plugins/BreweryX-3.6.0.jar"))

    testImplementation(libs.bundles.junit)
    testImplementation(project(":plugins:civmodcore-paper"))
    testImplementation(project(":plugins:combattagplus-paper"))
    testImplementation(libs.mockito.core)
}

tasks.test {
    useJUnitPlatform()
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