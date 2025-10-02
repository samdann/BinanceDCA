plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "binance-order-service"

include("core")
include("lambda")
include("ec2")
