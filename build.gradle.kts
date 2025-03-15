plugins {
    kotlin("jvm") version "2.0.21"
}

group = "ru.itis"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    /**
     * Versions
     */
    val jsoup = "1.18.3"
    val coroutines = "1.9.0"
    val compress = "1.23.0"

    /**
     * Dependencies
     */
    implementation("org.jsoup:jsoup:$jsoup")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines")
    implementation("org.apache.commons:commons-compress:$compress")
}

kotlin {
    jvmToolchain(17)
}