plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.serialization)
    id("application")
}

group = "smf.samurai1"
version = "1.0.2"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.sqlite.jdbc)
    implementation(libs.tinylog.api)
    implementation(libs.tinylog.impl)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.tgbot.api)
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

application {
    mainClass.set("dev.scroogemcfawk.manicurebot.AppKt")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
    jvmTargetValidationMode.set(org.jetbrains.kotlin.gradle.dsl.jvm.JvmTargetValidationMode.ERROR)
}

tasks.jar {
    archiveBaseName.set(project.name)
}
