plugins {
    java
    kotlin("jvm") version "1.3.72"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.slf4j", "slf4j-simple", "1.+")
    testImplementation("com.codeborne", "selenide", "5.12.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.+")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.+")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.+")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_13
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "13"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "13"
    }

    test {
        useJUnitPlatform()
    }
}

