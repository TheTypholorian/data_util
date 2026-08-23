plugins {
    kotlin("jvm") version "2.4.0"
}

group = "net.typho"
version = "1.0.0"

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