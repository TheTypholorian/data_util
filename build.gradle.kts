plugins {
    kotlin("jvm") version "2.4.0"
    id("net.typho.typho_publish") version "1.0.4"
}

group = "net.typho"
version = "2.5.1"

repositories {
    mavenCentral()
    maven("https://typho.net/maven")
}

dependencies {
    implementation(kotlin("reflect"))
    implementation("net.typho:parsing_util:2.0.0")
}

kotlin {
    jvmToolchain(8)
}