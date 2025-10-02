plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow") version "8.1.1"
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

    // AWS Lambda
    implementation("com.amazonaws:aws-lambda-java-core:1.2.3")
    implementation("com.amazonaws:aws-lambda-java-events:3.11.3")

    // Jackson (for Lambda JSON handling)
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.shadowJar {
    archiveBaseName.set("binance-lambda")
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