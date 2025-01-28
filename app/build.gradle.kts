plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
}

android {
    namespace = "com.arny.mobilebert"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.arny.mobilebert"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }

    packagingOptions {
        // Используем pickFirst для указанных библиотек
        pickFirst("lib/x86/libc++_shared.so")
        pickFirst("lib/x86_64/libc++_shared.so")
        pickFirst("lib/armeabi-v7a/libc++_shared.so")
        pickFirst("lib/arm64-v8a/libc++_shared.so")

         exclude("META-INF/LICENSE")
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Configure APK file name
    applicationVariants.all {
        val variant = this
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                val outputFileName = "MobileBert-${variant.baseName}-${variant.versionName}-${variant.versionCode}" +
                        ".apk"
                output.outputFileName = outputFileName
            }
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets["main"].java.srcDirs("src/main/java")
    sourceSets["main"].jniLibs.srcDirs("src/main/jniLibs")

    aaptOptions {
        noCompress += "tflite"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.lifecycle)
    implementation(libs.androidx.fragmentKtx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.kotlinx.coroutines.core)

    implementation( libs.dagger)
    implementation(libs.dagger.android)
    implementation(libs.dagger.android.support)
    kapt(libs.dagger.compiler)
    kapt(libs.dagger.android.processor)

    implementation(libs.glide)

    implementation(libs.tensorflow)
    implementation(libs.tensorflow.lite.support)
    implementation(libs.tensorflow.lite.metadata)
    implementation(libs.tensorflow.lite.select.tf.ops)
    implementation(libs.tensorflow.lite.gpu)
    implementation("org.tensorflow:tensorflow-lite:0.0.0-nightly-SNAPSHOT")
    implementation("ai.djl.sentencepiece:sentencepiece:0.31.1")
    implementation("ai.djl:api:0.31.1")
    implementation("org.slf4j:slf4j-simple:2.0.7")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// Allow references to generated code
kapt {
    correctErrorTypes = true
}

kotlin {
    sourceSets {
        val main by getting {
            kotlin.srcDir("build/generated/ksp/main/kotlin")
        }
    }
}