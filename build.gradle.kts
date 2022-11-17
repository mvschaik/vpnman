import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import cz.habarta.typescript.generator.JsonLibrary
import cz.habarta.typescript.generator.TypeScriptFileType
import cz.habarta.typescript.generator.TypeScriptOutputKind

plugins {
    id("org.springframework.boot") version "2.7.2"
    id("io.spring.dependency-management") version "1.0.12.RELEASE"
    id("cz.habarta.typescript-generator") version "2.32.889"
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.spring") version "1.6.21"
}

group = "nl.smeh"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-mustache")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
//    implementation("cz.habarta.typescript-generator:typescript-generator-gradle-plugin:2.32.889")
//    implementation("cz.habarta.typescript-generator:typescript-generator-spring:2.32.889")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
//    developmentOnly("cz.habarta.typescript-generator:typescript-generator-maven-plugin")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:1.12.5")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks {
    generateTypeScript {
        jsonLibrary = JsonLibrary.jackson2
        classes = listOf("nl.smeh.vpnman.IpDhcpServerLease")
        outputKind = TypeScriptOutputKind.module
        outputFileType = TypeScriptFileType.implementationFile
     }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
