// Root project — only declares plugin versions, doesn't apply them.
// Each module opts in to the plugins it needs.
plugins {
    id("com.android.application") version "8.13.2" apply false
    id("com.android.library") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("org.jetbrains.kotlin.jvm") version "1.9.24" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24" apply false
    // KSP runs Room's annotation processor at build time. Version is paired
    // with Kotlin 1.9.24 — bumping Kotlin requires bumping this too.
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
}
