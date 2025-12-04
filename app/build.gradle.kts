import java.util.Properties
// import kotlin.apply

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt.gradle)
    alias(libs.plugins.google.gms.google.services)
}
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun getSecret(name: String, defaultValue: String = ""): String {
    val value = localProperties.getProperty(name) ?: System.getenv(name) ?: defaultValue
    return value.trim()
}

val plaidClientId = getSecret("PLAID_CLIENT_ID")
val plaidSecret = getSecret("PLAID_SECRET")
val plaidClientName = getSecret("PLAID_CLIENT_NAME", "Walletify")

android {
    namespace = "hu.ait.walletify"
    compileSdk = 36


    defaultConfig {
        applicationId = "hu.ait.walletify"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "PLAID_CLIENT_ID", "\"$plaidClientId\"")
        buildConfigField("String", "PLAID_SECRET", "\"$plaidSecret\"")
        buildConfigField("String", "PLAID_CLIENT_NAME", "\"$plaidClientName\"")
        buildConfigField("String", "PLAID_ENV", "\"sandbox\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.material)

    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.material3.adaptive.navigation3)
    implementation(libs.kotlinx.serialization.core)
    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.kotlinx.serialization)
    implementation(libs.serialization.converter)
    // http build request to help retro and then unit testing later
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    testImplementation(libs.kotlinx.coroutines.test)
    implementation(libs.sdk.core)
    implementation("com.google.firebase:firebase-functions-ktx:20.4.0")

}