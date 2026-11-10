plugins {
    kotlin("jvm") version "2.4.0"
    id("net.typho.typho_publish") version "1.0.4"
}

group = "net.typho"
version = "2.5.0"

repositories {
    mavenCentral()
    maven("https://typho.net/maven")
}

dependencies {
    implementation(kotlin("reflect"))
    implementation("net.typho:parsing_util:1.0.1")
}

kotlin {
    jvmToolchain(8)
}