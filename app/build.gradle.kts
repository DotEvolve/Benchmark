plugins {
    alias(libs.plugins.android.application)

    id("com.google.gms.google-services")

    id("com.google.firebase.crashlytics")
}

android {
    namespace = "net.dotevolve.benchmark"
    compileSdk = 36

    defaultConfig {
        applicationId = "net.dotevolve.benchmark"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics.ndk)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.perf)
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    testImplementation(libs.junit)

    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}