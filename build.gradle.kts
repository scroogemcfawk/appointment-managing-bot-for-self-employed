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
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.tinylog)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.tgbot.api)
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
    jvmTargetValidationMode.set(org.jetbrains.kotlin.gradle.dsl.jvm.JvmTargetValidationMode.ERROR)
}

tasks.register<Jar>("fatJar") {
    group = "build"

    archiveBaseName.set(project.name)

    dependsOn(configurations.runtimeClasspath)

    from(
        sourceSets.main.get().output,
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    )

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest.attributes(
        "Main-Class" to "smf.samurai1.AppKt"
    )
}

tasks.register<Task>("appId") {
    println("${rootProject.name}:$version")
}
