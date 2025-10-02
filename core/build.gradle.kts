plugins {
    kotlin("jvm")
}

group = "com.blackchain"
version = "1.0.0"

dependencies {
    // Http4k
    val httpVersion = "5.10.2.0"
    implementation("org.http4k:http4k-core:${httpVersion}")
    implementation("org.http4k:http4k-client-okhttp:${httpVersion}")
    implementation("org.http4k:http4k-format-jackson:${httpVersion}")

    // Result4k
    implementation("dev.forkhandles:result4k:2.17.0.0")

    // Jackson for JSON
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")

    // Environment variables from .env file
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")

    // Logging
    implementation("org.slf4j:slf4j-simple:2.0.7")

    // Test dependencies
    testImplementation("org.http4k:http4k-testing-strikt:${httpVersion}")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
