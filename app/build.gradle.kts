// app/build.gradle.kts (Este é o arquivo dentro da pasta 'app')

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17" // Compatível com Kotlin 1.9.22 e Java 17
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // Kotlin BOM: MUDANDO PARA 1.9.22
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.9.22"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // ESTRATÉGIA DE RESOLUÇÃO: FORÇAR kotlin-stdlib para 1.9.22
    // Isso é crucial para tentar resolver conflitos de dependência intransigentes
    configurations.all {
        resolutionStrategy.force("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
        resolutionStrategy.force("org.jetbrains.kotlin:kotlin-stdlib-common:1.9.22")
        resolutionStrategy.force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.22")
        resolutionStrategy.force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22")
        // Você pode adicionar mais forçar aqui se outras bibliotecas Kotlin específicas continuarem aparecendo
        // em versões incompatíveis (ex: kotlinx-coroutines-core)
    }

    // Dependências do AndroidX e Material Design
    implementation(libs.material)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.activity)

    // Dependência Lifecycle ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.1")

    // Room components
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Retrofit e Gson para APIs Web
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    debugImplementation("com.squareup.okhttp3:logging-interceptor:4.10.0")

    // Outras dependências do seu projeto (mantidas como estavam)
    implementation("com.google.mlkit:text-recognition:16.0.0")
    // photoview: agora com o projeto em Kotlin 1.9.22 e força no stdlib, deve ser compatível
    implementation("io.getstream:photoview:1.0.3")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // ViewPager2
    implementation("androidx.viewpager2:viewpager2:1.1.0")

    // MPAndroidChart
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Testes
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
