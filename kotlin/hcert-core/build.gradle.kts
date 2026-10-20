// The HCERT library: implements :hcert-api with the Kotlin standard library
// and the platform only (JDK on JVM and Android, node:crypto and node:zlib on
// Node). No third-party runtime dependency. [STT-GEN-004, STT-SEC-002]
plugins {
    kotlin("multiplatform")
}

group = "who.smart.trust" // placeholder coordinates (OI-14)
version = "0.1.0-SNAPSHOT"

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    js(IR) {
        nodejs()
        useEsModules()
        binaries.library()
        generateTypeScriptDefinitions()
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":hcert-api"))
        }
        jvmTest.dependencies {
            implementation(kotlin("test-junit"))
        }
    }
}

tasks.withType<Test>().configureEach {
    // The conformance vectors and the test PKI live at the repository root.
    systemProperty("repo.root", rootProject.projectDir.parentFile.absolutePath)
}
