
buildscript {
    extra.apply {
        set("kotlinVersion", "1.7.0")
        set("composeVersion", "1.2.0")
        set("roomVersion", "2.4.3")
    }
    dependencies {
        classpath("com.google.gms:google-services:4.3.13")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.1")
    }
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "7.2.2" apply false
    id("com.android.library") version "7.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.6.10" apply false
    id("com.google.devtools.ksp") version "1.7.0-1.0.6" apply false
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.buildDir)
}
