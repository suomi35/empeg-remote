plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.chasinglemons.empeg"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.chasinglemons.empeg"
        minSdk = 23
        targetSdk = 36
        versionCode = 7
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation(libs.timber)

    implementation(libs.jsoup)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.navigation.compose)
    implementation(libs.compose.material3)
    implementation(libs.androidx.material3.window.size.class1.android)
    implementation(libs.coil.compose)
    implementation(libs.coil.network)

//    implementation(libs.okhttp)

//    implementation("io.coil-kt.coil3", "coil-compose", "3.2.0") {
////        version { // the version block replaces the version above (so that one is redundant when used)
////            branch = "main" // if you want to specify a branch instead of a version
////        }
//    }
//    implementation("io.coil-kt.coil3", "coil-network-okhttp", "3.2.0") {
////        version { // the version block replaces the version above (so that one is redundant when used)
////            branch = "main" // if you want to specify a branch instead of a version
////        }
//    }

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}