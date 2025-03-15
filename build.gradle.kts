plugins {
    kotlin("jvm") version "2.0.21"
}

group = "ru.itis"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {}

kotlin {
    jvmToolchain(17)
}