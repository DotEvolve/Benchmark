import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
    alias(libs.plugins.google.firebase.perf)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    alias(libs.plugins.jacoco)
    alias(libs.plugins.sonarqube)
}

android {
    namespace = "net.dotevolve.benchmark"
    compileSdk = 36

    defaultConfig {
        applicationId = "net.dotevolve.benchmark"
        minSdk = 23
        targetSdk = 36
        versionCode = 16
        versionName = "16"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            versionNameSuffix = "-RELEASE"

            // Enables code-related app optimization.
            isMinifyEnabled = true

            // Enables resource shrinking.
            isShrinkResources = true

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

            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
            resValue("string", "app_name", "Benchmark.DEBUG")
            resValue("string", "admob_app_id", "ca-app-pub-3940256099942544~3347511713")
            resValue("string", "admob_banner_ad_unit_id", "ca-app-pub-3940256099942544/9214589741")
            resValue("string", "admob_interstitial_ad_unit_id", "ca-app-pub-3940256099942544/1033173712")

        }
    }

    firebaseCrashlytics {
        nativeSymbolUploadEnabled = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }

    lint {
        baseline = file("lint-baseline.xml")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {

    implementation(libs.admob.ads)
    implementation(libs.admob.banners)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.crashlytics.ndk)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.appcheck)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.firebase.perf)
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)
    implementation(libs.play.app.update)
    implementation(libs.profileinstaller)
    implementation(libs.activity.ktx)
    implementation(libs.firebase.firestore)

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.leanback)
    implementation(libs.firebase.messaging)
    implementation(libs.core.ktx)
    implementation(libs.work.runtime)

    // Jetpack Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.ui.tooling)

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

tasks.register<Exec>("publishToAptoide") {
    dependsOn("assembleRelease")
    group = "publishing"
    description = "Uploads the release APK to Aptoide."

    val properties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        properties.load(localPropertiesFile.inputStream())
    }

    val apiKey = properties.getProperty("aptoide.apiKey")
    if (apiKey.isNullOrEmpty()) {
        throw GradleException("Aptoide API key not found. Please add 'aptoide.apiKey=YOUR_KEY' to your local.properties file.")
    }

    val apkPath = "${project.layout.buildDirectory.get()}/outputs/apk/release/app-release.apk"
    val apkFile = project.file(apkPath)
    if (!apkFile.exists()) {
        throw GradleException("Release APK not found at $apkPath. Please run the assembleRelease task first.")
    }
    println("Uploading $apkPath to Aptoide...")

    commandLine(
        "curl",
        "-X", "POST",
        "https://uploader.catappult.io/api",
        "-H", "Api-Key: $apiKey",
        "-F", "apk=@$apkPath"
    )

    doLast {
        println("Upload command finished.")
    }
}
