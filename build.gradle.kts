plugins {
    kotlin("jvm") version "1.9.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}
group = "com.cryptotracker"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // AWS Lambda
    implementation("com.amazonaws:aws-lambda-java-core:1.2.3")
    implementation("com.amazonaws:aws-lambda-java-events:3.11.3")

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

tasks.compileKotlin {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}

tasks.compileTestKotlin {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

application {
    mainClass = "BinanceOrderHandler"
}

// Shadow JAR configuration for AWS Lambda
tasks.shadowJar {
    archiveBaseName.set("binance-order-service")
    archiveVersion.set("1.0.0")
    archiveClassifier.set("all")
    mergeServiceFiles()

    // Exclude unnecessary files to reduce JAR size
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

