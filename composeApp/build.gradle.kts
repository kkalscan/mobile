plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
                }
            }
        }
    }
    wasmJs {
        browser {
            binaries.executable()
            commonWebpackConfig {
                outputFileName = "kkalscan.js"
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.decompose)
            implementation(libs.decompose.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.appmetrica.analytics)
            implementation("androidx.browser:browser:1.8.0")
            implementation("androidx.exifinterface:exifinterface:1.3.7")
            implementation("androidx.health.connect:connect-client:1.1.0-rc02")
        }
        val androidInstrumentedTest by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.compose.ui.test.junit4)
                implementation(libs.androidx.test.ext.junit)
                implementation(libs.androidx.test.runner)
            }
        }
    }
}

android {
    namespace = "ru.kkalscan.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        applicationId = "ru.kkalscan.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = (findProperty("VERSION_CODE") as String?)?.toIntOrNull() ?: 1
        versionName = findProperty("VERSION_NAME") as String? ?: "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD").orEmpty()
                keyAlias = System.getenv("ANDROID_KEY_ALIAS").orEmpty()
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD").orEmpty()
            }
        }
    }
    buildTypes {
        debug {
            buildConfigField("String", "APPMETRICA_API_KEY", "\"\"")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("String", "APPMETRICA_API_KEY", "\"102bc19a-c1d7-4aa6-a8a6-da5c8c3a1fc0\"")
            val keystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
            if (!keystorePath.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.7.8")
}
