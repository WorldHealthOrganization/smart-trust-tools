plugins {
    kotlin("multiplatform") version "2.0.21" apply false
}

// Kotlin/JS uses the Node.js already on the machine; the plugin downloads no
// Node.js or Yarn distribution. [AGENTS.md: dependencies are pinned and verified]
plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>().download = false
}
// The npm packages Kotlin/JS needs (its mocha test runner) are installed only
// from the committed kotlin-js-store/yarn.lock, with integrity hashes and no
// install scripts; a lock that would change fails the build.
plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().apply {
        download = false
        ignoreScripts = true
        yarnLockMismatchReport = org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport.FAIL
        reportNewYarnLock = true
        yarnLockAutoReplace = false
    }
}

// Release packaging (STT-NFR-001, STT-NFR-002; docs/RELEASING.md). Nothing
// here publishes to a remote: the only repository is a directory in this
// build, which `publishDryRun` fills and lists. The version is read from the
// repository's single version source, VERSION, which
// tools/release-packaging/check-version.mjs checks against Cargo.toml and the
// npm package.json files. Only Gradle's built-in maven-publish is used, so
// verification-metadata.xml gains nothing.
val releaseVersion = rootProject.file("../../VERSION").readText().trim()
val dryRunRepo = layout.buildDirectory.dir("maven-dry-run")

subprojects {
    // io.github.litlfred is a namespace the owner can verify on the Sonatype
    // Central Portal with the GitHub account; who.smart.trust, the Kotlin
    // package root, cannot be verified by anyone but WHO (OI-14).
    group = "io.github.litlfred"
    version = releaseVersion

    // Byte-identical archives from the same commit.
    tasks.withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        apply(plugin = "maven-publish")
        configure<PublishingExtension> {
            repositories {
                maven {
                    name = "dryRun"
                    url = uri(dryRunRepo)
                }
            }
            publications.withType<MavenPublication>().configureEach {
                // Maven Central requires a javadoc jar for every publication.
                // Without Dokka (a new plugin) it holds the module's README,
                // which Central accepts; the KDoc is in the sources jar.
                val pub = name
                val javadocJar = tasks.register<Jar>("${pub}JavadocJar") {
                    archiveBaseName.set("${project.name}-$pub")
                    archiveClassifier.set("javadoc")
                    from(rootProject.file("README.md"))
                }
                artifact(javadocJar)
                pom {
                    name.set("SMART Trust HCERT ${project.name}")
                    description.set(
                        if (project.name == "hcert-api")
                            "SMART Trust HCERT API: the Kotlin declarations of the HCERT and VHL contract, for the JVM, Android and Node."
                        else
                            "SMART Trust HCERT: issue and verify WHO GDHCN health certificates and Verifiable Health Links, for the JVM, Android and Node."
                    )
                    url.set("https://github.com/litlfred/smart-trust-tools")
                    licenses {
                        license {
                            name.set("Apache-2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                            distribution.set("repo")
                        }
                    }
                    developers {
                        developer {
                            id.set("litlfred")
                            name.set("Carl Leitner")
                            url.set("https://github.com/litlfred")
                        }
                    }
                    scm {
                        url.set("https://github.com/litlfred/smart-trust-tools")
                        connection.set("scm:git:https://github.com/litlfred/smart-trust-tools.git")
                        developerConnection.set("scm:git:ssh://git@github.com/litlfred/smart-trust-tools.git")
                    }
                    issueManagement {
                        system.set("GitHub")
                        url.set("https://github.com/litlfred/smart-trust-tools/issues")
                    }
                }
            }
        }
    }
}

// Empty the dry-run repository, publish every module into it, and list the
// artefacts a Central Portal bundle would hold, before signing (each also has
// .md5, .sha1, .sha256 and .sha512 files beside it; see docs/RELEASING.md).
val cleanDryRun by tasks.registering(Delete::class) {
    delete(dryRunRepo)
}
val publishDryRun by tasks.registering {
    group = "publishing"
    description = "Publish every module into build/maven-dry-run (no remote) and list the files."
    dependsOn(cleanDryRun)
    dependsOn(subprojects.map { "${it.path}:publishAllPublicationsToDryRunRepository" })
    val repo = dryRunRepo
    doLast {
        val root = repo.get().asFile
        root.walkTopDown()
            .filter { it.isFile && !it.name.startsWith("maven-metadata") && it.extension !in setOf("md5", "sha1", "sha256", "sha512") }
            .map { it.relativeTo(root).invariantSeparatorsPath }.sorted()
            .forEach { println(it) }
    }
}
subprojects {
    // Every upload, not only the aggregate task, so the clean never follows one.
    tasks.withType<PublishToMavenRepository>().configureEach { mustRunAfter(cleanDryRun) }
}
