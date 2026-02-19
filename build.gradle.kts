// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "9.0.1" apply false
    id("com.android.library") version "9.0.1" apply false
    id("com.google.devtools.ksp") version "2.3.5" apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
    id("com.google.firebase.crashlytics") version "3.0.6" apply false
    id("org.jetbrains.kotlinx.kover") version "0.9.1" apply false
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.layout.buildDirectory)
}
