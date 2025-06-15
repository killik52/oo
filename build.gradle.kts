// build.gradle.kts (Este é o arquivo na RAIZ do seu projeto)

buildscript {
    repositories {
        google()       // Repositório do Google (para plugins Android)
        mavenCentral() // Repositório Maven Central
    }
    dependencies {
        // Android Gradle Plugin: Usar 8.3.0
        classpath("com.android.tools.build:gradle:8.3.0")

        // Plugin Kotlin Gradle: MUDANDO PARA 1.9.22 (Versão mais estável da série 1.9.x)
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")

        // Plugin KSP (Kotlin Symbol Processing): Alinhado com Kotlin 1.9.22
        // A versão para Kotlin 1.9.22 é geralmente "1.9.22-1.0.17"
        classpath("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:1.9.22-1.0.17")
    }
}

plugins {
    // Plugin de aplicação Android: Versão 8.3.0
    id("com.android.application") version "8.3.0" apply false

    // Plugin Kotlin para Android: Versão 1.9.22
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false

    // Plugin KSP: Versão 1.9.22-1.0.17
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
