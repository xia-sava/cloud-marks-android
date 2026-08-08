
import java.util.Properties

/**
 * リリース署名の情報を releaseSigningConfigs.properties から読む．
 * 手元にファイルを置かない環境では環境変数から受け取る．
 */
val releaseSigningProperties = file("releaseSigningConfigs.properties")
    .takeIf { it.exists() }
    ?.let { propertiesFile ->
        Properties().apply { propertiesFile.inputStream().use { load(it) } }
    }

fun signingValue(key: String, environmentVariable: String): String? =
    releaseSigningProperties?.getProperty(key)
        ?: providers.environmentVariable(environmentVariable).orNull


plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.10"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.10"
    id("org.jetbrains.kotlinx.kover")
}

android {

    compileSdk = 36
    defaultConfig {
        applicationId = "to.sava.cloudmarksandroid"
        minSdk = 30
        targetSdk = 36
        //noinspection HighAppVersionCode
        versionCode = 2026021901
        versionName = "1.3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            signingValue("storeFile", "CLOUD_MARKS_RELEASE_KEYSTORE")?.let { path ->
                storeFile = file(path)
                storePassword =
                    signingValue("storePassword", "CLOUD_MARKS_RELEASE_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "CLOUD_MARKS_RELEASE_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "CLOUD_MARKS_RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled  = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("release")
            isJniDebuggable = true
            isMinifyEnabled = false
            isDebuggable = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/gradle/incremental.annotation.processors"
            excludes += "/META-INF/INDEX.LIST"
        }
    }
    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
        // android.util.Log のような Android フレームワークの呼び出しを既定値で通す
        unitTests.isReturnDefaultValues = true
    }
    namespace = "to.sava.cloudmarksandroid"
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        freeCompilerArgs.addAll("-Xjvm-default=all", "-opt-in=kotlin.RequiresOptIn")
    }
}

composeCompiler {
    includeComposeMappingFile.set(false)
    stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("stability_config.conf"))
}

dependencies {

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.compose.ui:ui:1.10.3")
    implementation("androidx.navigation:navigation-compose:2.9.7")
    implementation("androidx.compose.material:material:1.10.3")
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.compose.runtime:runtime-livedata:1.10.3")
    implementation("androidx.compose.ui:ui-tooling-preview:1.10.3")
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.datastore:datastore-preferences:1.2.0")
    implementation("androidx.work:work-runtime-ktx:2.11.1")

    implementation("androidx.room:room-runtime:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")

    implementation(platform("io.insert-koin:koin-bom:4.1.1"))
    implementation("io.insert-koin:koin-android")
    implementation("io.insert-koin:koin-androidx-workmanager")
    implementation("io.insert-koin:koin-androidx-compose")


    implementation("aws.sdk.kotlin:s3:1.6.19")

    implementation("io.ktor:ktor-client-core:3.3.1")
    implementation("io.ktor:ktor-client-okhttp:3.3.1")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    implementation("io.coil-kt:coil-compose:2.7.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.3")
    testImplementation("org.junit.jupiter:junit-jupiter-params:6.0.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.0.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.ktor:ktor-client-mock:3.3.1")
    testImplementation("io.mockk:mockk:1.14.9")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.10.3")

    debugImplementation("androidx.compose.ui:ui-tooling:1.10.3")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.10.3")
}
