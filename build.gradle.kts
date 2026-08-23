plugins {
    kotlin("jvm") version "2.4.0"
    `maven-publish`
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "typho"
            url = uri(layout.projectDirectory.dir("../website/maven"))
        }
    }
}