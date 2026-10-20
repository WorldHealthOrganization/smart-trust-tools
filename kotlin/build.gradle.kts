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
