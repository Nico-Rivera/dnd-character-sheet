plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // @Serializable annotations live on every domain class so the data layer
    // (added in a later commit) can JSON-encode them into Room columns without
    // a separate DTO tree. 'api' so downstream modules can serialize domain
    // types without redeclaring the dep.
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // CharacterRepository exposes Flow on its observe* methods, so coroutines
    // is part of the domain contract (not just an implementation detail).
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    testImplementation("junit:junit:4.13.2")
}
