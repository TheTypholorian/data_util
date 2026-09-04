plugins {
    kotlin("jvm") version "2.4.0"
    id("net.typho.typho_publish") version "1.0.1"
}

group = "net.typho"
version = "1.2.7"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("reflect"))
    testImplementation("com.google.code.gson:gson:2.14.0")
}

kotlin {
    jvmToolchain(8)
}