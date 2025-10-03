// ============================================
// File: ec2/build.gradle.kts
// ============================================
plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

group = "com.blackchain"
version = "1.0.0"

dependencies {
    // Depend on core module
    implementation(project(":core"))

    // Http4k
    val httpVersion = "5.10.2.0"
    implementation("org.http4k:http4k-core:${httpVersion}")
    implementation("org.http4k:http4k-client-okhttp:${httpVersion}")
    implementation("org.http4k:http4k-format-jackson:${httpVersion}")

    // Result4k
    implementation("dev.forkhandles:result4k:2.17.0.0")
}

application {
    mainClass.set("com.blackchain.ec2.MainKt")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}


tasks.shadowJar {
    archiveBaseName.set("binance-ec2-dca")
    archiveVersion.set("1.0.0")
    archiveClassifier.set("all")
    mergeServiceFiles()

    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}