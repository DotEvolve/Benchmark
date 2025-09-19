plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
    alias(libs.plugins.google.firebase.perf)

    id("jacoco")
    id("org.sonarqube") version "6.3.1.5724"
}

android {
    namespace = "net.dotevolve.benchmark"
    compileSdk = 36

    defaultConfig {
        applicationId = "net.dotevolve.benchmark"
        minSdk = 34
        targetSdk = 36
        versionCode = 5
        versionName = "4.0-hotfix"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            versionNameSuffix = "-RELEASE"

            // Enables code-related app optimization.
            isMinifyEnabled = true

            // Enables resource shrinking.
            isShrinkResources = true

            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
            resValue("string", "admob_app_id", "ca-app-pub-3940256099942544~3347511713")
            resValue("string", "admob_banner_ad_unit_id", "ca-app-pub-3940256099942544/9214589741")
            resValue("string", "admob_interstitial_ad_unit_id", "ca-app-pub-3940256099942544/1033173712")

        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.crashlytics.ndk)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.appcheck)
    implementation(libs.firebase.appcheck.playintegrity)
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

tasks.withType<Test>().configureEach {
    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

tasks.register<JacocoReport>("jacocoTestReportDebug") {
    description = "Generates Jacoco code coverage reports for the debug build."
    group = "verification"

    dependsOn("testDebugUnitTest", "createDebugCoverageReport")

    sourceDirectories.setFrom(files("$projectDir/src/main/java"))
    classDirectories.setFrom(
        fileTree("$layout.buildDir/tmp/kotlin-classes/debug") {
            exclude(
                "**/R.class",
                "**/R\$*.class",
                "**/BuildConfig.*",
                "**/Manifest*.*",
                "**/*\$ViewBinder*.*", // For ViewBinding
                "**/*\$ViewBinding*.*", // For ViewBinding
                "**/*Module*.*", // Exclude Hilt/Dagger modules if any
                "**/*Factory*.*", // Exclude Hilt/Dagger factories if any
                "**/*_MembersInjector*.*" // Exclude Hilt/Dagger injectors if any
            )
        }
    )
    executionData.setFrom(
        files(
            layout.buildDirectory.file("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec").get().asFile,
            layout.buildDirectory.file("outputs/code_coverage/debugAndroidTest/connected/*coverage.ec").get().asFile
        )
    )

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}
