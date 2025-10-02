plugins {
    kotlin("jvm") version "1.9.20" apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    group = "com.blackchain"
    version = "1.0.0"
}