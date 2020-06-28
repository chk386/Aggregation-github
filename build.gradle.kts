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
    // brew install ffmpeg 실행하기전에 ffmpeg 코덱 설치 필수
//    testImplementation("com.automation-remarks", "video-recorder-junit5", "1.+")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.+")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_12
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "12"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "12"
    }

    test {
        useJUnitPlatform()
    }
}

