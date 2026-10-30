plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  val releaseKeystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
  val releaseKeystoreFile = file(releaseKeystorePath)
  val hasReleaseKeystore = releaseKeystoreFile.exists() && releaseKeystoreFile.isFile

  defaultConfig {
    applicationId = "com.aistudio.dailyhabits.krvmdf"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  val debugKeystoreFile = file("${rootDir}/debug.keystore")
  val hasDebugKeystore = debugKeystoreFile.exists() && debugKeystoreFile.isFile

  signingConfigs {
    create("release") {
      if (hasReleaseKeystore) {
        storeFile = releaseKeystoreFile
        storePassword = System.getenv("STORE_PASSWORD")
      } else if (hasDebugKeystore) {
        storeFile = debugKeystoreFile
        storePassword = "android"
      }
      keyAlias = if (hasReleaseKeystore) "upload" else "androiddebugkey"
      keyPassword = if (hasReleaseKeystore) System.getenv("KEY_PASSWORD") else "android"
    }
    create("debugConfig") {
      if (hasDebugKeystore) {
        storeFile = debugKeystoreFile
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = if (hasReleaseKeystore) signingConfigs.getByName("release") else if (hasDebugKeystore) signingConfigs.getByName("debugConfig") else signingConfigs.getByName("debug")
    }
    debug {
      if (hasDebugKeystore) {
        signingConfig = signingConfigs.getByName("debugConfig")
      }
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
}
