plugins {
    kotlin("jvm") version "2.4.0"
    id("net.typho.typho_publish") version "1.0.4"
}

group = "net.typho"
version = "2.3.7"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("reflect"))
}

kotlin {
    jvmToolchain(8)
}