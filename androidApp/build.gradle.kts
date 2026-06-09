import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { input ->
        localProperties.load(input)
    }
}

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(projects.shared)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodelCompose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "org.example.project"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "org.example.project"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        buildConfigField(
            "String",
            "DOUBAO_API_KEY",
                "\"${localProperties.getProperty("DOUBAO_API_KEY", "")}\""
        )

        buildConfigField(
            "String",
            "DOUBAO_MODEL",
            "\"${localProperties.getProperty("DOUBAO_MODEL", "")}\""
        )

        buildConfigField(
            "String",
            "DOUBAO_VISION_MODEL",
            "\"${localProperties.getProperty("DOUBAO_VISION_MODEL", "doubao-seed-2-0-mini-260428")}\""
        )

        buildConfigField(
            "String",
            "DOUBAO_IMAGE_MODEL",
            "\"${localProperties.getProperty("DOUBAO_IMAGE_MODEL", "doubao-seedream-5-0-260128")}\""
        )

        buildConfigField(
            "String",
            "DOUBAO_TTS_APP_KEY",
            "\"${localProperties.getProperty("DOUBAO_TTS_APP_KEY", "")}\""
        )

        buildConfigField(
            "String",
            "DOUBAO_TTS_ACCESS_KEY",
            "\"${localProperties.getProperty("DOUBAO_TTS_ACCESS_KEY", "")}\""
        )

        buildConfigField(
            "String",
            "DOUBAO_TTS_RESOURCE_ID",
            "\"${localProperties.getProperty("DOUBAO_TTS_RESOURCE_ID", "seed-tts-2.0")}\""
        )

        buildConfigField(
            "String",
            "DOUBAO_TTS_SPEAKER",
            "\"${localProperties.getProperty("DOUBAO_TTS_SPEAKER", "zh_female_vv_uranus_bigtts")}\""
        )

        buildConfigField(
            "String",
            "WEB_SEARCH_API_KEY",
            "\"${localProperties.getProperty("WEB_SEARCH_API_KEY", "")}\""
        )

        buildConfigField(
            "String",
            "WEB_SEARCH_API_KEY_ID",
            "\"${localProperties.getProperty("WEB_SEARCH_API_KEY_ID", "")}\""
        )

        buildConfigField(
            "String",
            "DOUBAO_ASR_APP_KEY",
            "\"${localProperties.getProperty("DOUBAO_ASR_APP_KEY", "")}\""
        )

        buildConfigField(
            "String",
            "DOUBAO_ASR_ACCESS_KEY",
            "\"${localProperties.getProperty("DOUBAO_ASR_ACCESS_KEY", "")}\""
        )

        buildConfigField(
            "String",
            "DOUBAO_ASR_RESOURCE_ID",
            "\"${localProperties.getProperty("DOUBAO_ASR_RESOURCE_ID", "volc.bigasr.auc_turbo")}\""
        )
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
