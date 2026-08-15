plugins {
    kotlin("jvm") version "2.4.0"
}

group = "net.typho"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("com.google.code.gson:gson:2.14.0")
}

kotlin {
    jvmToolchain(8)
}

tasks.test {
    useJUnitPlatform()
}