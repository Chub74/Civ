import io.papermc.paperweight.tasks.RemapJar
import io.papermc.paperweight.userdev.PaperweightUserExtension
import xyz.jpenilla.runpaper.task.RunServer

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "eclipse")

    // Gradle's Eclipse model derives scope from runtime usage, so a compileOnly(project(...))
    // dependency that is also a testImplementation gets marked test-only. Eclipse then hides it
    // from main sources and every cross-plugin type fails to resolve. Keep library scoping intact.
    configure<org.gradle.plugins.ide.eclipse.model.EclipseModel> {
        classpath.file.whenMerged {
            (this as org.gradle.plugins.ide.eclipse.model.Classpath).entries
                .filterIsInstance<org.gradle.plugins.ide.eclipse.model.ProjectDependency>()
                .forEach { it.entryAttributes.remove("test") }
        }
    }

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
        withSourcesJar()
        withJavadocJar()
    }

    tasks.withType<Javadoc> {
        options {
            (this as CoreJavadocOptions).addBooleanOption("Xdoclint:none", true)
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release = 25
    }

    tasks.withType<ProcessResources> {
        filteringCharset = "UTF-8"
    }

    tasks.withType<Javadoc> {
        enabled = false
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    configure<PublishingExtension> {
        val githubActor = System.getenv("GITHUB_ACTOR")
        val githubToken = System.getenv("GITHUB_TOKEN")

        repositories {
            if (githubActor != null && githubToken != null) {
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/CivMC/Civ")
                    credentials {
                        username = githubActor
                        password = githubToken
                    }
                }
            }
        }

        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
            }
            pluginManager.withPlugin("com.gradleup.shadow") {
                create<MavenPublication>("shadow") {
                    from(components["java"])
                }
            }
        }
    }

    tasks.withType<RunServer> {
        minecraftVersion("1.18")
    }

    pluginManager.withPlugin("io.papermc.paperweight.userdev") {
        // 26.1+ ships unobfuscated and has no reobf mappings
        if (libs.versions.paper.get().startsWith("26.")) {
            tasks.named<RemapJar>("reobfJar") {
                enabled = false
            }

            // MOJANG_PRODUCTION used to hand the unclassified jar to reobfJar, which consumed
            // shadowJar's output. With reobf off that jar falls back to the plain `jar` task, so
            // the artifact everyone deploys loses its shaded dependencies and the plugin dies at
            // enable with NoClassDefFoundError. Do the classifier swap paperweight was doing.
            pluginManager.withPlugin("com.gradleup.shadow") {
                tasks.named<Jar>("jar") {
                    archiveClassifier.set("dev")
                }
                tasks.named<Jar>("shadowJar") {
                    archiveClassifier.set("")
                }
            }
        } else {
            project.extensions.getByName<PaperweightUserExtension>("paperweight")
                .reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION
        }

        tasks.withType<ProcessResources> {
            filesMatching("plugin.yml") {
                expand(project.properties)
            }
        }
    }

    pluginManager.withPlugin("com.gradleup.shadow") {
        tasks {
            named("build") {
                dependsOn("shadowJar")
            }
            named("assemble") {
                dependsOn("shadowJar")
            }
        }
    }
}
