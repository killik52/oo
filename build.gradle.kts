// build.gradle.kts (Este é o arquivo na RAIZ do seu projeto)

buildscript {
    repositories {
        google()       // Repositório do Google (para plugins Android)
        mavenCentral() // Repositório Maven Central
    }
    dependencies {
        // Android Gradle Plugin: Usando a versão estável 8.3.0
        classpath("com.android.tools.build:gradle:8.3.0") // CORRIGIDO

        // Plugin Kotlin Gradle: Versão 2.0.20
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.20")

        // Plugin KSP (Kotlin Symbol Processing): Alinhado com Kotlin 2.0.20
        classpath("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.0.20-1.0.25")
    }
}

plugins {
    // Plugin de aplicação Android: Usando a versão estável 8.3.0
    id("com.android.application") version "8.3.0" apply false // CORRIGIDO

    // Plugin Kotlin para Android: Versão 2.0.20
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false

    // Plugin KSP: Versão 2.0.20-1.0.25
    id("com.google.devtools.ksp") version "2.0.20-1.0.25" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
