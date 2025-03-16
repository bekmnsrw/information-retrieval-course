plugins {
    kotlin("jvm") version "2.0.21"
}

group = "ru.itis"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    /**
     * Versions
     */
    val jsoup = "1.18.3"
    val coroutines = "1.9.0"
    val compress = "1.23.0"
    val aot = "2025.02.15"

    /**
     * Dependencies
     */
    implementation("org.jsoup:jsoup:$jsoup")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines")
    implementation("org.apache.commons:commons-compress:$compress")
    implementation("com.github.demidko:aot:$aot")
}

kotlin {
    jvmToolchain(17)
}