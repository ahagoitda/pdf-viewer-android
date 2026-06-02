import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(FileInputStream(localPropertiesFile))
    }
}

val uploadStoreFile = localProperties.getProperty("UPLOAD_STORE_FILE")
val uploadKeyAlias = localProperties.getProperty("UPLOAD_KEY_ALIAS")
val uploadStorePassword = localProperties.getProperty("UPLOAD_STORE_PASSWORD")
val uploadKeyPassword = localProperties.getProperty("UPLOAD_KEY_PASSWORD")
val admobAppId = localProperties.getProperty("ADMOB_APP_ID", "")
val admobManifestAppId = admobAppId.ifBlank { "ca-app-pub-3940256099942544~3347511713" }
val hasReleaseSigningConfig = listOf(
    uploadStoreFile,
    uploadKeyAlias,
    uploadStorePassword,
    uploadKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.pdfutility"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pdfutility"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "ADMOB_APP_ID", "\"$admobAppId\"")
        buildConfigField("String", "ADMOB_BANNER_AD_UNIT_ID", "\"${localProperties.getProperty("ADMOB_BANNER_AD_UNIT_ID", "")}\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_AD_UNIT_ID", "\"${localProperties.getProperty("ADMOB_INTERSTITIAL_AD_UNIT_ID", "")}\"")

        manifestPlaceholders["ADMOB_APP_ID"] = admobManifestAppId
    }

    signingConfigs {
        if (hasReleaseSigningConfig) {
            create("release") {
                storeFile = rootProject.file(uploadStoreFile!!)
                storePassword = uploadStorePassword
                keyAlias = uploadKeyAlias
                keyPassword = uploadKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            if (hasReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    implementation(libs.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    implementation(libs.datastore.preferences)

    implementation(libs.admob)

    implementation(libs.coil.compose)
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
}
