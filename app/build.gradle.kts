
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import java.io.FileInputStream
import java.util.Properties

val releaseSigningConfigsProperties = Properties().also {
    it.load(FileInputStream(file("releaseSigningConfigs.properties")))
}
fun Properties.str(key: String): String = this[key] as String


plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.google.devtools.ksp")
}

android {

    compileSdk = 34
    defaultConfig {
        applicationId = "to.sava.cloudmarksandroid"
        minSdk = 30
        targetSdk = 34
        versionCode = 701
        versionName = "0.7.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs  {
        create("release") {
            keyAlias = releaseSigningConfigsProperties.str("keyAlias")
            keyPassword = releaseSigningConfigsProperties.str("keyPassword")
            storeFile = file(releaseSigningConfigsProperties.str("storeFile"))
            storePassword = releaseSigningConfigsProperties.str("storePassword")
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
            isRenderscriptDebuggable = true
            isJniDebuggable =true
            isDebuggable = true
            configure<CrashlyticsExtension> {
                nativeSymbolUploadEnabled = true
                mappingFileUploadEnabled = false
                strippedNativeLibsDir = "build/ndklibs/obj"
                unstrippedNativeLibsDir = "build/ndklibs/libs"
            }
        }
    }
    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_17)
        targetCompatibility(JavaVersion.VERSION_17)
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = freeCompilerArgs + "-Xjvm-default=all" + "-opt-in=kotlin.RequiresOptIn"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/gradle/incremental.annotation.processors"
        }
    }
    namespace = "to.sava.cloudmarksandroid"
    applicationVariants.configureEach {
        kotlin.sourceSets {
            getByName(name) {
                kotlin.srcDir("build/generated/ksp/${name}/kotlin")
            }
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.compose.ui:ui:1.5.0")
    implementation("androidx.navigation:navigation-compose:2.7.0")
    implementation("androidx.compose.material:material:1.5.0")
    implementation("androidx.compose.material:material-icons-core:1.5.0")
    implementation("androidx.compose.material:material-icons-extended:1.5.0")
    implementation("androidx.compose.runtime:runtime-livedata:1.5.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.0")
    implementation("androidx.activity:activity-compose:1.7.2")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.work:work-runtime-ktx:2.8.1")

    implementation("androidx.room:room-runtime:2.5.2")
    annotationProcessor("androidx.room:room-compiler:2.5.2")
    ksp("androidx.room:room-compiler:2.5.2")
    implementation("androidx.room:room-ktx:2.5.2")

    implementation("io.insert-koin:koin-android:3.4.3")
    implementation("io.insert-koin:koin-androidx-workmanager:3.4.3")
    implementation("io.insert-koin:koin-androidx-compose:3.4.6")
    implementation("io.insert-koin:koin-androidx-compose-navigation:3.4.6")
    implementation("io.insert-koin:koin-compose:1.0.4")
    runtimeOnly("io.insert-koin:koin-annotations:1.2.2")
    implementation("io.insert-koin:koin-ksp-compiler:1.2.2")
    ksp("io.insert-koin:koin-ksp-compiler:1.2.2")

    implementation("com.google.accompanist:accompanist-permissions:0.23.1")
    implementation("com.google.android.gms:play-services-auth:20.6.0")
    implementation("com.google.api-client:google-api-client-android:1.26.0") {
        exclude("org.apache.httpcomponents")
    }
    implementation("com.google.apis:google-api-services-drive:v3-rev20211107-1.32.1") {
        exclude("org.apache.httpcomponents")
    }
    implementation("io.coil-kt:coil-compose:2.1.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.0")

    debugImplementation("androidx.compose.ui:ui-tooling:1.5.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.0")
}
